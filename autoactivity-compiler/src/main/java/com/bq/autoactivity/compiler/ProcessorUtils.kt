package com.bq.autoactivity.compiler

import com.bq.autoactivity.AutoActivity
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun <T> T.typeMirrors(property: KProperty1<T, Array<KClass<*>>>): List<TypeMirror> {
    try {
        property.get(this)
    } catch(ex: MirroredTypesException) {
        return ex.typeMirrors
    } catch (ex: MirroredTypeException) {
        return listOf(ex.typeMirror)
    }
    throw IllegalArgumentException("Value is not a Class<?>[]")
}