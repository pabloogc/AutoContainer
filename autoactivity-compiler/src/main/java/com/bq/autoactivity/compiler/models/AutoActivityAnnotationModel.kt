package com.bq.autoactivity.compiler.models

import com.bq.autoactivity.AutoActivity
import com.bq.autoactivity.compiler.typeMirrors
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Element

class AutoActivityAnnotationModel(val element: Element,
                                  val annotation: AutoActivity) {

    val modules: List<TypeName>
    val dependencies: List<TypeName>

    init {
        modules = annotation.typeMirrors(AutoActivity::modules).map { ClassName.get(it) }
        dependencies = annotation.typeMirrors(AutoActivity::dependencies).map { ClassName.get(it) }
    }
}