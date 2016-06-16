package com.bq.autoactivity.compiler;

import com.bq.autoactivity.AutoActivity;
import com.google.auto.service.AutoService;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;


@AutoService(Processor.class)
public class AutoActivityService extends AbstractProcessor {
    private final AutoActivityProcessor delegate = AutoActivityProcessor.INSTANCE;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        delegate.init(processingEnv);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(AutoActivity.class.getCanonicalName());
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
