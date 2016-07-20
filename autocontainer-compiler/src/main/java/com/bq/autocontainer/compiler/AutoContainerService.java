package com.bq.autocontainer.compiler;

import com.bq.autocontainer.AutoContainer;
import com.bq.autocontainer.Callback;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;


@AutoService(Processor.class)
public class AutoContainerService extends AbstractProcessor {
    private final AutoContainerProcessor delegate = AutoContainerProcessor.INSTANCE;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        delegate.init(processingEnv);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(AutoContainer.class.getCanonicalName());
        set.add(Callback.class.getCanonicalName());
        set.add(com.bq.autocontainer.Plugin.class.getCanonicalName());
        return set;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return delegate.process(annotations, roundEnv);
    }
}
