package com.cleawing.ignite.akka;


import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RegistryRemoveProcessor implements EntryProcessor<UUID, Set<String>, Set<String>> {
    @Override public Set<String> process(MutableEntry<UUID, Set<String>> e, Object... args) {
        String systemName = (String)args[0];
        Set<String> newVal;

        if (e.exists()) {
            Set<String> oldVal = e.getValue();
            oldVal.remove(systemName);
            newVal = oldVal;
        } else {
            newVal = new HashSet<>();
        }
        e.setValue(newVal);
        return newVal;
    }
}
