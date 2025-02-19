package de.xab.porter.core;

import de.xab.porter.api.dataconnection.SinkConnection;
import de.xab.porter.api.dataconnection.SrcConnection;
import de.xab.porter.api.exception.PorterException;
import de.xab.porter.api.task.Context;
import de.xab.porter.common.spi.ExtensionLoader;
import de.xab.porter.transfer.channel.Channel;
import de.xab.porter.transfer.reader.Reader;
import de.xab.porter.transfer.writer.Writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * atomic unit of a transmission action, may split up by {@link Session}
 */
public class Task {
    private Context context;
    private Reader reader;
    private List<Map.Entry<Writer, Channel>> writers;

    public Task(Context context) {
        this.context = context;
    }

    public void init() {
        final SrcConnection srcConnection = context.getSrcConnection();
        this.reader = ExtensionLoader.getExtensionLoader().loadExtension(srcConnection.getType(), Reader.class);
        this.reader.setChannels(new ArrayList<>());
        register();
        //todo split
    }

    /**
     * construct relations among reader, writer and its channel, define the action when channel is ready to write
     */
    public void register() {
        final List<SinkConnection> sinkConnections = context.getSinkConnections();
        this.writers = sinkConnections.stream().
                map(sink -> {
                    final Writer writer = ExtensionLoader.getExtensionLoader().
                            loadExtension(sink.getType(), Writer.class);
                    final Object dataSource = writer.getDataSource(sink);
                    final Object connection = writer.connect(sink, writer.getDataSource(sink));
                    final Channel channel = ExtensionLoader.getExtensionLoader().
                            loadExtension(this.context.getProperties().getChannel(), Channel.class);
                    channel.setOnReadListener(data -> writer.write(connection, dataSource, sink, data));
                    reader.getChannels().add(channel);
                    return Map.entry(writer, channel);
                }).collect(Collectors.toList());
        registerProperties();
    }

    /**
     * init source behavior by sinks properties
     */
    private void registerProperties() {
        final SrcConnection srcConnection = context.getSrcConnection();
        final SrcConnection.Properties srcConnectionProperties = srcConnection.getProperties();
        final List<SinkConnection> sinkConnections = context.getSinkConnections();
        srcConnectionProperties.setCreate(
                sinkConnections.stream().map(SinkConnection::getProperties).
                        anyMatch(SinkConnection.Properties::isCreate));
    }

    /**
     * start a transmission task
     */
    public void start() {
        final Object dataSource = reader.getDataSource(context.getSrcConnection());
        Object connection = null;
        try {
            connection = reader.connect(context.getSrcConnection(), dataSource);
            reader.read(connection, context);
        } catch (PorterException e) {
            throw new PorterException("reader start failed", e);
        } finally {
            reader.close(connection, dataSource);
        }
    }
}
