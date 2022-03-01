package cn.powernukkitx.powerdoc.processor;

import cn.powernukkitx.powerdoc.Book;
import java.util.HashMap;
import java.util.Map;

public interface Processor {
    String getName();

    String getId();

    void work(Book book);

    Map<String, Class<? extends Processor>> registeredProcessor = new HashMap<>();

    static void registerProcessor(String id, Class<? extends Processor> processorClass) {
        registeredProcessor.put(id, processorClass);
    }

    static Class<? extends Processor> getProcessorClass(String id) {
        if(registeredProcessor.size() == 0) {
            initInnerProcessor();
        }
        return registeredProcessor.get(id);
    }

    static void initInnerProcessor() {
        registerProcessor("copy-file", CopyFileProcessor.class);
    }
}
