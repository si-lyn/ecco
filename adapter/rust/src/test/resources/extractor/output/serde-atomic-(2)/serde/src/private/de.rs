use crate::lib::*;

use crate::de::value::{BorrowedBytesDeserializer, BytesDeserializer};
use crate::de::{
    Deserialize, DeserializeSeed, Deserializer, EnumAccess, Error, IntoDeserializer, VariantAccess,
    Visitor,
};



pub use crate::serde_core_private::InPlaceSeed;

/// If the missing field is of type `Option<T>` then treat is as `None`,
/// otherwise it is an error.
pub fn missing_field<'de, V, E>(field: &'static str) -> Result<V, E>
where
    V: Deserialize<'de>,
    E: Error,
{
    struct MissingFieldDeserializer<E>(&'static str, PhantomData<E>);

    #[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
    impl<'de, E> Deserializer<'de> for MissingFieldDeserializer<E>
    where
        E: Error,
    {
        type Error = E;

        fn deserialize_any<V>(self, _visitor: V) -> Result<V::Value, E>
        where
            V: Visitor<'de>,
        {
            Err(Error::missing_field(self.0))
        }

        fn deserialize_option<V>(self, visitor: V) -> Result<V::Value, E>
        where
            V: Visitor<'de>,
        {
            visitor.visit_none()
        }

        serde_core::forward_to_deserialize_any! {
            bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string
            bytes byte_buf unit unit_struct newtype_struct seq tuple
            tuple_struct map struct enum identifier ignored_any
        }
    }

    let deserializer = MissingFieldDeserializer(field, PhantomData);
    Deserialize::deserialize(deserializer)
}




////////////////////////////////////////////////////////////////////////////////

// Like `IntoDeserializer` but also implemented for `&[u8]`. This is used for
// the newtype fallthrough case of `field_identifier`.
//
//    #[derive(Deserialize)]
//    #[serde(field_identifier)]
//    enum F {
//        A,
//        B,
//        Other(String), // deserialized using IdentifierDeserializer
//    }
pub trait IdentifierDeserializer<'de, E: Error> {
    type Deserializer: Deserializer<'de, Error = E>;

    fn from(self) -> Self::Deserializer;
}

pub struct Borrowed<'de, T: 'de + ?Sized>(pub &'de T);

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl<'de, E> IdentifierDeserializer<'de, E> for u64
where
    E: Error,
{
    type Deserializer = <u64 as IntoDeserializer<'de, E>>::Deserializer;

    fn from(self) -> Self::Deserializer {
        self.into_deserializer()
    }
}

pub struct StrDeserializer<'a, E> {
    value: &'a str,
    marker: PhantomData<E>,
}

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl<'de, 'a, E> Deserializer<'de> for StrDeserializer<'a, E>
where
    E: Error,
{
    type Error = E;

    fn deserialize_any<V>(self, visitor: V) -> Result<V::Value, Self::Error>
    where
        V: Visitor<'de>,
    {
        visitor.visit_str(self.value)
    }

    serde_core::forward_to_deserialize_any! {
        bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string
        bytes byte_buf option unit unit_struct newtype_struct seq tuple
        tuple_struct map struct enum identifier ignored_any
    }
}

pub struct BorrowedStrDeserializer<'de, E> {
    value: &'de str,
    marker: PhantomData<E>,
}

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl<'de, E> Deserializer<'de> for BorrowedStrDeserializer<'de, E>
where
    E: Error,
{
    type Error = E;

    fn deserialize_any<V>(self, visitor: V) -> Result<V::Value, Self::Error>
    where
        V: Visitor<'de>,
    {
        visitor.visit_borrowed_str(self.value)
    }

    serde_core::forward_to_deserialize_any! {
        bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string
        bytes byte_buf option unit unit_struct newtype_struct seq tuple
        tuple_struct map struct enum identifier ignored_any
    }
}

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl<'a, E> IdentifierDeserializer<'a, E> for &'a str
where
    E: Error,
{
    type Deserializer = StrDeserializer<'a, E>;

    fn from(self) -> Self::Deserializer {
        StrDeserializer {
            value: self,
            marker: PhantomData,
        }
    }
}

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl<'de, E> IdentifierDeserializer<'de, E> for Borrowed<'de, str>
where
    E: Error,
{
    type Deserializer = BorrowedStrDeserializer<'de, E>;

    fn from(self) -> Self::Deserializer {
        BorrowedStrDeserializer {
            value: self.0,
            marker: PhantomData,
        }
    }
}

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl<'a, E> IdentifierDeserializer<'a, E> for &'a [u8]
where
    E: Error,
{
    type Deserializer = BytesDeserializer<'a, E>;

    fn from(self) -> Self::Deserializer {
        BytesDeserializer::new(self)
    }
}

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl<'de, E> IdentifierDeserializer<'de, E> for Borrowed<'de, [u8]>
where
    E: Error,
{
    type Deserializer = BorrowedBytesDeserializer<'de, E>;

    fn from(self) -> Self::Deserializer {
        BorrowedBytesDeserializer::new(self.0)
    }
}










pub struct AdjacentlyTaggedEnumVariantSeed<F> {
    pub enum_name: &'static str,
    pub variants: &'static [&'static str],
    pub fields_enum: PhantomData<F>,
}

pub struct AdjacentlyTaggedEnumVariantVisitor<F> {
    enum_name: &'static str,
    fields_enum: PhantomData<F>,
}

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl<'de, F> Visitor<'de> for AdjacentlyTaggedEnumVariantVisitor<F>
where
    F: Deserialize<'de>,
{
    type Value = F;

    fn expecting(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        write!(formatter, "variant of enum {}", self.enum_name)
    }

    fn visit_enum<A>(self, data: A) -> Result<Self::Value, A::Error>
    where
        A: EnumAccess<'de>,
    {
        let (variant, variant_access) = tri!(data.variant());
        tri!(variant_access.unit_variant());
        Ok(variant)
    }
}

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl<'de, F> DeserializeSeed<'de> for AdjacentlyTaggedEnumVariantSeed<F>
where
    F: Deserialize<'de>,
{
    type Value = F;

    fn deserialize<D>(self, deserializer: D) -> Result<Self::Value, D::Error>
    where
        D: Deserializer<'de>,
    {
        deserializer.deserialize_enum(
            self.enum_name,
            self.variants,
            AdjacentlyTaggedEnumVariantVisitor {
                enum_name: self.enum_name,
                fields_enum: PhantomData,
            },
        )
    }
}

