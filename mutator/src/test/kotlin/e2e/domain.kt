package de.peekandpoke.ultra.mutator.e2e

import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.e2e.package1.FirstInPackage1
import de.peekandpoke.ultra.mutator.e2e.package1.SecondInPackage1
import de.peekandpoke.ultra.mutator.e2e.package2.FirstInPackage2
import de.peekandpoke.ultra.mutator.e2e.package2.SecondInPackage2
import java.time.LocalDate

@Mutable
data class Generic<T, X>(
    val one: T,
    val two: X
)

@Mutable
data class GenericTypeInAction(
    val generic1: Generic<FirstInPackage1, FirstInPackage2>,
    val generic2: Generic<FirstInPackage2, Generic<FirstInPackage1, FirstInPackage2>>
)

@Mutable
data class ReferencingOtherPackages(
    val firstIn1: FirstInPackage1,
    val secondIn1: SecondInPackage1?,
    val firstIn2: FirstInPackage2,
    val secondIn2: SecondInPackage2?
)

@Mutable
data class WithNestedClass(val nested: InnerClass) {
    @Mutable
    data class InnerClass(val text: String)
}

@Mutable
data class WithDate(
    val date: LocalDate
)

@Mutable
data class WithListOfDates(
    val dates: List<LocalDate>
)

@Mutable
data class WithScalars(
    val aString: String = "string",
    val aChar: Char = 'c',
    val aByte: Byte = 1,
    val aShort: Short = 1,
    val aInt: Int = 1,
    val aLong: Long = 1L,
    val aFloat: Float = 1.0f,
    val aDouble: Double = 1.0,
    val aBool: Boolean = true
)

@Mutable
data class WithNullableScalars(
    val aString: String? = null,
    val aChar: Char? = null,
    val aByte: Byte? = null,
    val aShort: Short? = null,
    val aInt: Int? = null,
    val aLong: Long? = null,
    val aFloat: Float? = null,
    val aDouble: Double? = null,
    val aBool: Boolean? = null
)

@Mutable
data class WithAnyObject(
    val anObject: Any
)

@Mutable
data class WithAnyNullableObject(
    val anObject: Any?
)

@Mutable
data class Company(val name: String, val boss: Person)

data class Person(val name: String, val age: Int, val address: Address)

data class Address(val city: String, val zip: String)

@Mutable
data class ListOfAddresses(val addresses: List<Address>)

@Mutable
data class SetOfAddresses(val addresses: Set<Address>)

@Mutable
data class MapOfAddresses(val addresses: Map<String, Address>)

@Mutable
data class ListOfBools(val values: List<Boolean>)

@Mutable
data class ListOfChars(val values: List<Char>)

@Mutable
data class ListOfBytes(val values: List<Byte>)

@Mutable
data class ListOfShorts(val values: List<Short>)

@Mutable
data class ListOfInts(val values: List<Int>)

@Mutable
data class ListOfNullableInts(val values: List<Int?>)

@Mutable
data class ListOfLongs(val values: List<Long>)

@Mutable
data class ListOfFloats(val values: List<Float>)

@Mutable
data class ListOfDoubles(val values: List<Double>)

@Mutable
data class ListOfStrings(val values: List<String>)
