package com.bq.autoactivity.compiler

import com.bq.autoactivity.AutoActivity
import com.bq.autoactivity.compiler.models.ComponentModel
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

object AutoActivityProcessor : AbstractProcessor() {

  lateinit var env: ProcessingEnvironment

  override fun init(env: ProcessingEnvironment) {
    this.env = env
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    roundEnv.getElementsAnnotatedWith(AutoActivity::class.java)
        .let { ElementFilter.typesIn(it) } //Poor man's filter
        .filter { it.kind == ElementKind.INTERFACE } //Only interfaces
        .map { ComponentModel(it) }
        .forEach { it.generateClass() }
    return true
  }

}