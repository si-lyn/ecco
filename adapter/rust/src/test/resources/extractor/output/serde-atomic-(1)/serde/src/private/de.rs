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


pub struct StrDeserializer<'a, E> {
    value: &'a str,
    marker: PhantomData<E>,
}


pub struct BorrowedStrDeserializer<'de, E> {
    value: &'de str,
    marker: PhantomData<E>,
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



