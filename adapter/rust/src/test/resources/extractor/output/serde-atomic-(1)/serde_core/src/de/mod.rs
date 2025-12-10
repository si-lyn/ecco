//! Generic data structure deserialization framework.
//!
//! The two most important traits in this module are [`Deserialize`] and
//! [`Deserializer`].
//!
//!  - **A type that implements `Deserialize` is a data structure** that can be
//!    deserialized from any data format supported by Serde, and conversely
//!  - **A type that implements `Deserializer` is a data format** that can
//!    deserialize any data structure supported by Serde.
//!
//! # The Deserialize trait
//!
//! Serde provides [`Deserialize`] implementations for many Rust primitive and
//! standard library types. The complete list is below. All of these can be
//! deserialized using Serde out of the box.
//!
//! Additionally, Serde provides a procedural macro called [`serde_derive`] to
//! automatically generate [`Deserialize`] implementations for structs and enums
//! in your program. See the [derive section of the manual] for how to use this.
//!
//! In rare cases it may be necessary to implement [`Deserialize`] manually for
//! some type in your program. See the [Implementing `Deserialize`] section of
//! the manual for more about this.
//!
//! Third-party crates may provide [`Deserialize`] implementations for types
//! that they expose. For example the [`linked-hash-map`] crate provides a
//! [`LinkedHashMap<K, V>`] type that is deserializable by Serde because the
//! crate provides an implementation of [`Deserialize`] for it.
//!
//! # The Deserializer trait
//!
//! [`Deserializer`] implementations are provided by third-party crates, for
//! example [`serde_json`], [`serde_yaml`] and [`postcard`].
//!
//! A partial list of well-maintained formats is given on the [Serde
//! website][data formats].
//!
//! # Implementations of Deserialize provided by Serde
//!
//! This is a slightly different set of types than what is supported for
//! serialization. Some types can be serialized by Serde but not deserialized.
//! One example is `OsStr`.
//!
//!  - **Primitive types**:
//!    - bool
//!    - i8, i16, i32, i64, i128, isize
//!    - u8, u16, u32, u64, u128, usize
//!    - f32, f64
//!    - char
//!  - **Compound types**:
//!    - \[T; 0\] through \[T; 32\]
//!    - tuples up to size 16
//!  - **Common standard library types**:
//!    - String
//!    - Option\<T\>
//!    - Result\<T, E\>
//!    - PhantomData\<T\>
//!  - **Wrapper types**:
//!    - Box\<T\>
//!    - Box\<\[T\]\>
//!    - Box\<str\>
//!    - Cow\<'a, T\>
//!    - Cell\<T\>
//!    - RefCell\<T\>
//!    - Mutex\<T\>
//!    - RwLock\<T\>
//!    - Rc\<T\>&emsp;*(if* features = \["rc"\] *is enabled)*
//!    - Arc\<T\>&emsp;*(if* features = \["rc"\] *is enabled)*
//!  - **Collection types**:
//!    - BTreeMap\<K, V\>
//!    - BTreeSet\<T\>
//!    - BinaryHeap\<T\>
//!    - HashMap\<K, V, H\>
//!    - HashSet\<T, H\>
//!    - LinkedList\<T\>
//!    - VecDeque\<T\>
//!    - Vec\<T\>
//!  - **Zero-copy types**:
//!    - &str
//!    - &\[u8\]
//!  - **FFI types**:
//!    - CString
//!    - Box\<CStr\>
//!    - OsString
//!  - **Miscellaneous standard library types**:
//!    - Duration
//!    - SystemTime
//!    - Path
//!    - PathBuf
//!    - Range\<T\>
//!    - RangeInclusive\<T\>
//!    - Bound\<T\>
//!    - num::NonZero*
//!    - `!` *(unstable)*
//!  - **Net types**:
//!    - IpAddr
//!    - Ipv4Addr
//!    - Ipv6Addr
//!    - SocketAddr
//!    - SocketAddrV4
//!    - SocketAddrV6
//!
//! [Implementing `Deserialize`]: https://serde.rs/impl-deserialize.html
//! [`Deserialize`]: crate::Deserialize
//! [`Deserializer`]: crate::Deserializer
//! [`LinkedHashMap<K, V>`]: https://docs.rs/linked-hash-map/*/linked_hash_map/struct.LinkedHashMap.html
//! [`postcard`]: https://github.com/jamesmunns/postcard
//! [`linked-hash-map`]: https://crates.io/crates/linked-hash-map
//! [`serde_derive`]: https://crates.io/crates/serde_derive
//! [`serde_json`]: https://github.com/serde-rs/json
//! [`serde_yaml`]: https://github.com/dtolnay/serde-yaml
//! [derive section of the manual]: https://serde.rs/derive.html
//! [data formats]: https://serde.rs/#data-formats

use crate::lib::*;

////////////////////////////////////////////////////////////////////////////////

pub mod value;

mod ignored_any;
mod impls;

pub use self::ignored_any::IgnoredAny;

////////////////////////////////////////////////////////////////////////////////

macro_rules! declare_error_trait {
    (Error: Sized $(+ $($supertrait:ident)::+)*) => {
        /// The `Error` trait allows `Deserialize` implementations to create descriptive
        /// error messages belonging to the `Deserializer` against which they are
        /// currently running.
        ///
        /// Every `Deserializer` declares an `Error` type that encompasses both
        /// general-purpose deserialization errors as well as errors specific to the
        /// particular deserialization format. For example the `Error` type of
        /// `serde_json` can represent errors like an invalid JSON escape sequence or an
        /// unterminated string literal, in addition to the error cases that are part of
        /// this trait.
        ///
        /// Most deserializers should only need to provide the `Error::custom` method
        /// and inherit the default behavior for the other methods.
        ///
        /// # Example implementation
        ///
        /// The [example data format] presented on the website shows an error
        /// type appropriate for a basic JSON data format.
        ///
        /// [example data format]: https://serde.rs/data-format.html
        #[cfg_attr(
            not(no_diagnostic_namespace),
            diagnostic::on_unimplemented(
                message = "the trait bound `{Self}: serde::de::Error` is not satisfied",
            )
        )]
        pub trait Error: Sized $(+ $($supertrait)::+)* {
            /// Raised when there is general error when deserializing a type.
            ///
            /// The message should not be capitalized and should not end with a period.
            ///
            /// ```edition2021
            /// # use std::str::FromStr;
            /// #
            /// # struct IpAddr;
            /// #
            /// # impl FromStr for IpAddr {
            /// #     type Err = String;
            /// #
            /// #     fn from_str(_: &str) -> Result<Self, String> {
            /// #         unimplemented!()
            /// #     }
            /// # }
            /// #
            /// use serde::de::{self, Deserialize, Deserializer};
            ///
            /// impl<'de> Deserialize<'de> for IpAddr {
            ///     fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
            ///     where
            ///         D: Deserializer<'de>,
            ///     {
            ///         let s = String::deserialize(deserializer)?;
            ///         s.parse().map_err(de::Error::custom)
            ///     }
            /// }
            /// ```
            fn custom<T>(msg: T) -> Self
            where
                T: Display;

            /// Raised when a `Deserialize` receives a type different from what it was
            /// expecting.
            ///
            /// The `unexp` argument provides information about what type was received.
            /// This is the type that was present in the input file or other source data
            /// of the Deserializer.
            ///
            /// The `exp` argument provides information about what type was being
            /// expected. This is the type that is written in the program.
            ///
            /// For example if we try to deserialize a String out of a JSON file
            /// containing an integer, the unexpected type is the integer and the
            /// expected type is the string.
            #[cold]
            fn invalid_type(unexp: Unexpected, exp: &dyn Expected) -> Self {
                Error::custom(format_args!("invalid type: {}, expected {}", unexp, exp))
            }

            /// Raised when a `Deserialize` receives a value of the right type but that
            /// is wrong for some other reason.
            ///
            /// The `unexp` argument provides information about what value was received.
            /// This is the value that was present in the input file or other source
            /// data of the Deserializer.
            ///
            /// The `exp` argument provides information about what value was being
            /// expected. This is the type that is written in the program.
            ///
            /// For example if we try to deserialize a String out of some binary data
            /// that is not valid UTF-8, the unexpected value is the bytes and the
            /// expected value is a string.
            #[cold]
            fn invalid_value(unexp: Unexpected, exp: &dyn Expected) -> Self {
                Error::custom(format_args!("invalid value: {}, expected {}", unexp, exp))
            }

            /// Raised when deserializing a sequence or map and the input data contains
            /// too many or too few elements.
            ///
            /// The `len` argument is the number of elements encountered. The sequence
            /// or map may have expected more arguments or fewer arguments.
            ///
            /// The `exp` argument provides information about what data was being
            /// expected. For example `exp` might say that a tuple of size 6 was
            /// expected.
            #[cold]
            fn invalid_length(len: usize, exp: &dyn Expected) -> Self {
                Error::custom(format_args!("invalid length {}, expected {}", len, exp))
            }

            /// Raised when a `Deserialize` enum type received a variant with an
            /// unrecognized name.
            #[cold]
            fn unknown_variant(variant: &str, expected: &'static [&'static str]) -> Self {
                if expected.is_empty() {
                    Error::custom(format_args!(
                        "unknown variant `{}`, there are no variants",
                        variant
                    ))
                } else {
                    Error::custom(format_args!(
                        "unknown variant `{}`, expected {}",
                        variant,
                        OneOf { names: expected }
                    ))
                }
            }

            /// Raised when a `Deserialize` struct type received a field with an
            /// unrecognized name.
            #[cold]
            fn unknown_field(field: &str, expected: &'static [&'static str]) -> Self {
                if expected.is_empty() {
                    Error::custom(format_args!(
                        "unknown field `{}`, there are no fields",
                        field
                    ))
                } else {
                    Error::custom(format_args!(
                        "unknown field `{}`, expected {}",
                        field,
                        OneOf { names: expected }
                    ))
                }
            }

            /// Raised when a `Deserialize` struct type expected to receive a required
            /// field with a particular name but that field was not present in the
            /// input.
            #[cold]
            fn missing_field(field: &'static str) -> Self {
                Error::custom(format_args!("missing field `{}`", field))
            }

            /// Raised when a `Deserialize` struct type received more than one of the
            /// same field.
            #[cold]
            fn duplicate_field(field: &'static str) -> Self {
                Error::custom(format_args!("duplicate field `{}`", field))
            }
        }
    }
}



/// `Unexpected` represents an unexpected invocation of any one of the `Visitor`
/// trait methods.
///
/// This is used as an argument to the `invalid_type`, `invalid_value`, and
/// `invalid_length` methods of the `Error` trait to build error messages.
///
/// ```edition2021
/// # use std::fmt;
/// #
/// # use serde::de::{self, Unexpected, Visitor};
/// #
/// # struct Example;
/// #
/// # impl<'de> Visitor<'de> for Example {
/// #     type Value = ();
/// #
/// #     fn expecting(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
/// #         write!(formatter, "definitely not a boolean")
/// #     }
/// #
/// fn visit_bool<E>(self, v: bool) -> Result<Self::Value, E>
/// where
///     E: de::Error,
/// {
///     Err(de::Error::invalid_type(Unexpected::Bool(v), &self))
/// }
/// # }
/// ```
#[derive(Copy, Clone, PartialEq, Debug)]
pub enum Unexpected<'a> {
    /// The input contained a boolean value that was not expected.
    Bool(bool),

    /// The input contained an unsigned integer `u8`, `u16`, `u32` or `u64` that
    /// was not expected.
    Unsigned(u64),

    /// The input contained a signed integer `i8`, `i16`, `i32` or `i64` that
    /// was not expected.
    Signed(i64),

    /// The input contained a floating point `f32` or `f64` that was not
    /// expected.
    Float(f64),

    /// The input contained a `char` that was not expected.
    Char(char),

    /// The input contained a `&str` or `String` that was not expected.
    Str(&'a str),

    /// The input contained a `&[u8]` or `Vec<u8>` that was not expected.
    Bytes(&'a [u8]),

    /// The input contained a unit `()` that was not expected.
    Unit,

    /// The input contained an `Option<T>` that was not expected.
    Option,

    /// The input contained a newtype struct that was not expected.
    NewtypeStruct,

    /// The input contained a sequence that was not expected.
    Seq,

    /// The input contained a map that was not expected.
    Map,

    /// The input contained an enum that was not expected.
    Enum,

    /// The input contained a unit variant that was not expected.
    UnitVariant,

    /// The input contained a newtype variant that was not expected.
    NewtypeVariant,

    /// The input contained a tuple variant that was not expected.
    TupleVariant,

    /// The input contained a struct variant that was not expected.
    StructVariant,

    /// A message stating what uncategorized thing the input contained that was
    /// not expected.
    ///
    /// The message should be a noun or noun phrase, not capitalized and without
    /// a period. An example message is "unoriginal superhero".
    Other(&'a str),
}

impl<'a> fmt::Display for Unexpected<'a> {
    fn fmt(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        use self::Unexpected::*;
        match *self {
            Bool(b) => write!(formatter, "boolean `{}`", b),
            Unsigned(i) => write!(formatter, "integer `{}`", i),
            Signed(i) => write!(formatter, "integer `{}`", i),
            Float(f) => write!(formatter, "floating point `{}`", WithDecimalPoint(f)),
            Char(c) => write!(formatter, "character `{}`", c),
            Str(s) => write!(formatter, "string {:?}", s),
            Bytes(_) => formatter.write_str("byte array"),
            Unit => formatter.write_str("unit value"),
            Option => formatter.write_str("Option value"),
            NewtypeStruct => formatter.write_str("newtype struct"),
            Seq => formatter.write_str("sequence"),
            Map => formatter.write_str("map"),
            Enum => formatter.write_str("enum"),
            UnitVariant => formatter.write_str("unit variant"),
            NewtypeVariant => formatter.write_str("newtype variant"),
            TupleVariant => formatter.write_str("tuple variant"),
            StructVariant => formatter.write_str("struct variant"),
            Other(other) => formatter.write_str(other),
        }
    }
}


impl<'de, T> Expected for T
where
    T: Visitor<'de>,
{
    fn fmt(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        self.expecting(formatter)
    }
}

impl Expected for &str {
    fn fmt(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        formatter.write_str(self)
    }
}

impl Display for dyn Expected + '_ {
    fn fmt(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        Expected::fmt(self, formatter)
    }
}

////////////////////////////////////////////////////////////////////////////////


impl<T> DeserializeOwned for T where T: for<'de> Deserialize<'de> {}


impl<'de, T> DeserializeSeed<'de> for PhantomData<T>
where
    T: Deserialize<'de>,
{
    type Value = T;

    #[inline]
    fn deserialize<D>(self, deserializer: D) -> Result<T, D::Error>
    where
        D: Deserializer<'de>,
    {
        T::deserialize(deserializer)
    }
}

////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////////


impl<'de, A> SeqAccess<'de> for &mut A
where
    A: ?Sized + SeqAccess<'de>,
{
    type Error = A::Error;

    #[inline]
    fn next_element_seed<T>(&mut self, seed: T) -> Result<Option<T::Value>, Self::Error>
    where
        T: DeserializeSeed<'de>,
    {
        (**self).next_element_seed(seed)
    }

    #[inline]
    fn next_element<T>(&mut self) -> Result<Option<T>, Self::Error>
    where
        T: Deserialize<'de>,
    {
        (**self).next_element()
    }

    #[inline]
    fn size_hint(&self) -> Option<usize> {
        (**self).size_hint()
    }
}

////////////////////////////////////////////////////////////////////////////////


impl<'de, A> MapAccess<'de> for &mut A
where
    A: ?Sized + MapAccess<'de>,
{
    type Error = A::Error;

    #[inline]
    fn next_key_seed<K>(&mut self, seed: K) -> Result<Option<K::Value>, Self::Error>
    where
        K: DeserializeSeed<'de>,
    {
        (**self).next_key_seed(seed)
    }

    #[inline]
    fn next_value_seed<V>(&mut self, seed: V) -> Result<V::Value, Self::Error>
    where
        V: DeserializeSeed<'de>,
    {
        (**self).next_value_seed(seed)
    }

    #[inline]
    fn next_entry_seed<K, V>(
        &mut self,
        kseed: K,
        vseed: V,
    ) -> Result<Option<(K::Value, V::Value)>, Self::Error>
    where
        K: DeserializeSeed<'de>,
        V: DeserializeSeed<'de>,
    {
        (**self).next_entry_seed(kseed, vseed)
    }

    #[inline]
    fn next_entry<K, V>(&mut self) -> Result<Option<(K, V)>, Self::Error>
    where
        K: Deserialize<'de>,
        V: Deserialize<'de>,
    {
        (**self).next_entry()
    }

    #[inline]
    fn next_key<K>(&mut self) -> Result<Option<K>, Self::Error>
    where
        K: Deserialize<'de>,
    {
        (**self).next_key()
    }

    #[inline]
    fn next_value<V>(&mut self) -> Result<V, Self::Error>
    where
        V: Deserialize<'de>,
    {
        (**self).next_value()
    }

    #[inline]
    fn size_hint(&self) -> Option<usize> {
        (**self).size_hint()
    }
}

////////////////////////////////////////////////////////////////////////////////



////////////////////////////////////////////////////////////////////////////////

/// Converts an existing value into a `Deserializer` from which other values can
/// be deserialized.
///
/// # Lifetime
///
/// The `'de` lifetime of this trait is the lifetime of data that may be
/// borrowed from the resulting `Deserializer`. See the page [Understanding
/// deserializer lifetimes] for a more detailed explanation of these lifetimes.
///
/// [Understanding deserializer lifetimes]: https://serde.rs/lifetimes.html
///
/// # Example
///
/// ```edition2021
/// use serde::de::{value, Deserialize, IntoDeserializer};
/// use serde_derive::Deserialize;
/// use std::str::FromStr;
///
/// #[derive(Deserialize)]
/// enum Setting {
///     On,
///     Off,
/// }
///
/// impl FromStr for Setting {
///     type Err = value::Error;
///
///     fn from_str(s: &str) -> Result<Self, Self::Err> {
///         Self::deserialize(s.into_deserializer())
///     }
/// }
/// ```
pub trait IntoDeserializer<'de, E: Error = value::Error> {
    /// The type of the deserializer being converted into.
    type Deserializer: Deserializer<'de, Error = E>;

    /// Convert this value into a deserializer.
    fn into_deserializer(self) -> Self::Deserializer;
}

////////////////////////////////////////////////////////////////////////////////

/// Used in error messages.
///
/// - expected `a`
/// - expected `a` or `b`
/// - expected one of `a`, `b`, `c`
///
/// The slice of names must not be empty.
struct OneOf {
    names: &'static [&'static str],
}

impl Display for OneOf {
    fn fmt(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        match self.names.len() {
            0 => panic!(), // special case elsewhere
            1 => write!(formatter, "`{}`", self.names[0]),
            2 => write!(formatter, "`{}` or `{}`", self.names[0], self.names[1]),
            _ => {
                tri!(formatter.write_str("one of "));
                for (i, alt) in self.names.iter().enumerate() {
                    if i > 0 {
                        tri!(formatter.write_str(", "));
                    }
                    tri!(write!(formatter, "`{}`", alt));
                }
                Ok(())
            }
        }
    }
}

struct WithDecimalPoint(f64);

impl Display for WithDecimalPoint {
    fn fmt(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        struct LookForDecimalPoint<'f, 'a> {
            formatter: &'f mut fmt::Formatter<'a>,
            has_decimal_point: bool,
        }

        impl<'f, 'a> fmt::Write for LookForDecimalPoint<'f, 'a> {
            fn write_str(&mut self, fragment: &str) -> fmt::Result {
                self.has_decimal_point |= fragment.contains('.');
                self.formatter.write_str(fragment)
            }

            fn write_char(&mut self, ch: char) -> fmt::Result {
                self.has_decimal_point |= ch == '.';
                self.formatter.write_char(ch)
            }
        }

        if self.0.is_finite() {
            let mut writer = LookForDecimalPoint {
                formatter,
                has_decimal_point: false,
            };
            tri!(write!(writer, "{}", self.0));
            if !writer.has_decimal_point {
                tri!(formatter.write_str(".0"));
            }
        } else {
            tri!(write!(formatter, "{}", self.0));
        }
        Ok(())
    }
}

