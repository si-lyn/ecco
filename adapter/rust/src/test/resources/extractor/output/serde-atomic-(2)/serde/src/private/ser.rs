use crate::lib::*;

use crate::ser::{self, Impossible, Serialize, SerializeMap, SerializeStruct, Serializer};


/// Used to check that serde(getter) attributes return the expected type.
/// Not public API.
pub fn constrain<T: ?Sized>(t: &T) -> &T {
    t
}

/// Not public API.
pub fn serialize_tagged_newtype<S, T>(
    serializer: S,
    type_ident: &'static str,
    variant_ident: &'static str,
    tag: &'static str,
    variant_name: &'static str,
    value: &T,
) -> Result<S::Ok, S::Error>
where
    S: Serializer,
    T: Serialize,
{
    value.serialize(TaggedSerializer {
        type_ident,
        variant_ident,
        tag,
        variant_name,
        delegate: serializer,
    })
}

struct TaggedSerializer<S> {
    type_ident: &'static str,
    variant_ident: &'static str,
    tag: &'static str,
    variant_name: &'static str,
    delegate: S,
}

enum Unsupported {
    Boolean,
    Integer,
    Float,
    Char,
    String,
    ByteArray,
    Optional,
    Sequence,
    Tuple,
    TupleStruct,
    #[cfg(not(any(feature = "std", feature = "alloc")))]
    Enum,
}

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl Display for Unsupported {
    fn fmt(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        match *self {
            Unsupported::Boolean => formatter.write_str("a boolean"),
            Unsupported::Integer => formatter.write_str("an integer"),
            Unsupported::Float => formatter.write_str("a float"),
            Unsupported::Char => formatter.write_str("a char"),
            Unsupported::String => formatter.write_str("a string"),
            Unsupported::ByteArray => formatter.write_str("a byte array"),
            Unsupported::Optional => formatter.write_str("an optional"),
            Unsupported::Sequence => formatter.write_str("a sequence"),
            Unsupported::Tuple => formatter.write_str("a tuple"),
            Unsupported::TupleStruct => formatter.write_str("a tuple struct"),
            #[cfg(not(any(feature = "std", feature = "alloc")))]
            Unsupported::Enum => formatter.write_str("an enum"),
        }
    }
}

impl<S> TaggedSerializer<S>
where
    S: Serializer,
{
    fn bad_type(self, what: Unsupported) -> S::Error {
        ser::Error::custom(format_args!(
            "cannot serialize tagged newtype variant {}::{} containing {}",
            self.type_ident, self.variant_ident, what
        ))
    }
}










////////////////////////////////////////////////////////////////////////////////////////////////////




////////////////////////////////////////////////////////////////////////////////////////////////////




pub struct AdjacentlyTaggedEnumVariant {
    pub enum_name: &'static str,
    pub variant_index: u32,
    pub variant_name: &'static str,
}

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl Serialize for AdjacentlyTaggedEnumVariant {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_unit_variant(self.enum_name, self.variant_index, self.variant_name)
    }
}

// Error when Serialize for a non_exhaustive remote enum encounters a variant
// that is not recognized.
pub struct CannotSerializeVariant<T>(pub T);

#[cfg_attr(not(no_diagnostic_namespace), diagnostic::do_not_recommend)]
impl<T> Display for CannotSerializeVariant<T>
where
    T: Debug,
{
    fn fmt(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        write!(formatter, "enum variant cannot be serialized: {:?}", self.0)
    }
}

