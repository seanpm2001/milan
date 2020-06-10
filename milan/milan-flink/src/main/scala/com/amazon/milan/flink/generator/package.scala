package com.amazon.milan.flink

import com.amazon.milan.flink.types._
import com.amazon.milan.flink.typeutil._
import com.amazon.milan.program.Duration
import com.amazon.milan.typeutil.{FieldDescriptor, ObjectTypeDescriptor, TypeDescriptor}
import org.apache.flink.streaming.api.windowing.time.Time

import scala.language.implicitConversions


package object generator {

  implicit class FlinkGeneratorStringExtensions(s: String) {
    def strip: String =
      this.s.stripMargin.stripLineEnd.stripPrefix("\n")

    def indent(level: Int): String = {
      val prefix = Seq.tabulate(level)(_ => "  ").mkString
      this.s.lines.map(line => prefix + line).mkString("\n")
    }

    def indentTail(level: Int): String = {
      val prefix = Seq.tabulate(level)(_ => "  ").mkString
      this.s.lines.zipWithIndex.map {
        case (line, index) =>
          if (index == 0) {
            line
          }
          else {
            prefix + line
          }
      }.mkString("\n")
    }
  }

  implicit class FlinkGeneratorTypeDescriptorExtensions[_](t: TypeDescriptor[_]) {
    def toTerm: ClassName = {
      if (t.isTuple && t.genericArguments.isEmpty) {
        ClassName("Product")
      }
      else {
        ClassName(t.fullName)
      }
    }

    def getFlinkTypeFullName: String = {
      if (t.isInstanceOf[TupleRecordTypeDescriptor[_]]) {
        ArrayRecord.typeName
      }
      else {
        t.fullName
      }
    }

    def toFlinkTerm: ClassName =
      ClassName(this.getFlinkTypeFullName)

    /**
     * Gets a [[TypeDescriptor]] for the type as it is used in a Flink data stream.
     * When Milan creates a stream where the record type is named tuples, it uses the [[ArrayRecord]] class
     * rather than the Flink tuple type. A special [[TypeDescriptor]] is used to represent these record types.
     */
    def toFlinkRecordType: TypeDescriptor[_] = {
      if (this.t.isNamedTuple) {
        new TupleRecordTypeDescriptor[Any](this.t.fields)
      }
      else {
        this.t
      }
    }

    def toMilanRecordType: TypeDescriptor[_] = {
      if (this.t.isTupleRecord) {
        TypeDescriptor.createTuple[Product](this.t.fields.map(_.fieldType))
      }
      else {
        this.t
      }
    }

    /**
     * Gets a [[TypeDescriptor]] that describes the input type wrapped in a [[RecordWrapper]] with no key type.
     */
    def wrapped: TypeDescriptor[RecordWrapper[_, _]] = {
      this.wrappedWithKey(com.amazon.milan.typeutil.types.EmptyTuple)
    }

    /**
     * Gets a [[TypeDescriptor]] that describes the input type wrapped in a [[RecordWrapper]] with the specified
     * key type.
     */
    def wrappedWithKey(keyType: TypeDescriptor[_]): TypeDescriptor[RecordWrapper[_, _]] = {
      val fields = List(
        FieldDescriptor[Any]("value", this.t.asInstanceOf[TypeDescriptor[Any]]),
        FieldDescriptor[Any]("key", keyType.asInstanceOf[TypeDescriptor[Any]]),
        FieldDescriptor[Long]("sequenceNumber", com.amazon.milan.typeutil.types.Long)
      )

      new ObjectTypeDescriptor[RecordWrapper[_, _]](
        "com.amazon.milan.flink.types.RecordWrapper",
        List(this.t, keyType),
        fields)
    }
  }

  implicit class FlinkGeneratorDurationExtensions(duration: Duration) {
    def toFlinkTime: Time = Time.milliseconds(duration.milliseconds)
  }

}
