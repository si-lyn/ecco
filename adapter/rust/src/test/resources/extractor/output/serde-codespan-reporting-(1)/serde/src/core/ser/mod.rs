//! Generic data structure serialization framework.
//!
//! The two most important traits in this module are [`Serialize`] and
//! [`Serializer`].
//!
//!  - **A type that implements `Serialize` is a data structure** that can be
//!    serialized to any data format supported by Serde, and conversely
//!  - **A type that implements `Serializer` is a data format** that can
//!    serialize any data structure supported by Serde.
//!
//! # The Serialize trait
//!
//! Serde provides [`Serialize`] implementations for many Rust primitive and
//! standard library types. The complete list is below. All of these can be
//! serialized using Serde out of the box.
//!
//! Additionally, Serde provides a procedural macro called [`serde_derive`] to
//! automatically generate [`Serialize`] implementations for structs and enums
//! in your program. See the [derive section of the manual] for how to use this.
//!
//! In rare cases it may be necessary to implement [`Serialize`] manually for
//! some type in your program. See the [Implementing `Serialize`] section of the
//! manual for more about this.
//!
//! Third-party crates may provide [`Serialize`] implementations for types that
//! they expose. For example the [`linked-hash-map`] crate provides a
//! [`LinkedHashMap<K, V>`] type that is serializable by Serde because the crate
//! provides an implementation of [`Serialize`] for it.
//!
//! # The Serializer trait
//!
//! [`Serializer`] implementations are provided by third-party crates, for
//! example [`serde_json`], [`serde_yaml`] and [`postcard`].
//!
//! A partial list of well-maintained formats is given on the [Serde
//! website][data formats].
//!
//! # Implementations of Serialize provided by Serde
//!
//!  - **Primitive types**:
//!    - bool
//!    - i8, i16, i32, i64, i128, isize
//!    - u8, u16, u32, u64, u128, usize
//!    - f32, f64
//!    - char
//!    - str
//!    - &T and &mut T
//!  - **Compound types**:
//!    - \[T\]
//!    - \[T; 0\] through \[T; 32\]
//!    - tuples up to size 16
//!  - **Common standard library types**:
//!    - String
//!    - Option\<T\>
//!    - Result\<T, E\>
//!    - PhantomData\<T\>
//!  - **Wrapper types**:
//!    - Box\<T\>
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
//!  - **FFI types**:
//!    - CStr
//!    - CString
//!    - OsStr
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
//! [Implementing `Serialize`]: https://serde.rs/impl-serialize.html
//! [`LinkedHashMap<K, V>`]: https://docs.rs/linked-hash-map/*/linked_hash_map/struct.LinkedHashMap.html
//! [`Serialize`]: crate::Serialize
//! [`Serializer`]: crate::Serializer
//! [`postcard`]: https://github.com/jamesmunns/postcard
//! [`linked-hash-map`]: https://crates.io/crates/linked-hash-map
//! [`serde_derive`]: https://crates.io/crates/serde_derive
//! [`serde_json`]: https://github.com/serde-rs/json
//! [`serde_yaml`]: https://github.com/dtolnay/serde-yaml
//! [derive section of the manual]: https://serde.rs/derive.html
//! [data formats]: https://serde.rs/#data-formats

use crate::lib::*;

mod fmt;
mod impls;
mod impossible;

pub use self::impossible::Impossible;

#[cfg(not(any(feature = "std", no_core_error)))]
#[doc(no_inline)]
pub use core::error::Error as StdError;

////////////////////////////////////////////////////////////////////////////////

macro_rules! declare_error_trait {
    (Error: Sized $(+ $($supertrait:ident)::+)*) => {
        /// Trait used by `Serialize` implementations to generically construct
        /// errors belonging to the `Serializer` against which they are
        /// currently running.
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
                message = "the trait bound `{Self}: serde::ser::Error` is not satisfied",
            )
        )]
        pub trait Error: Sized $(+ $($supertrait)::+)* {
            /// Used when a [`Serialize`] implementation encounters any error
            /// while serializing a type.
            ///
            /// The message should not be capitalized and should not end with a
            /// period.
            ///
            /// For example, a filesystem [`Path`] may refuse to serialize
            /// itself if it contains invalid UTF-8 data.
            ///
            /// ```edition2021
            /// # struct Path;
            /// #
            /// # impl Path {
            /// #     fn to_str(&self) -> Option<&str> {
            /// #         unimplemented!()
            /// #     }
            /// # }
            /// #
            /// use serde::ser::{self, Serialize, Serializer};
            ///
            /// impl Serialize for Path {
            ///     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
            ///     where
            ///         S: Serializer,
            ///     {
            ///         match self.to_str() {
            ///             Some(s) => serializer.serialize_str(s),
            ///             None => Err(ser::Error::custom("path contains invalid UTF-8 characters")),
            ///         }
            ///     }
            /// }
            /// ```
            ///
            /// [`Path`]: std::path::Path
            /// [`Serialize`]: crate::Serialize
            fn custom<T>(msg: T) -> Self
            where
                T: Display;
        }
    }
}


#[cfg(not(feature = "std"))]
declare_error_trait!(Error: Sized + Debug + Display);

////////////////////////////////////////////////////////////////////////////////

/// A **data structure** that can be serialized into any data format supported
/// by Serde.
///
/// Serde provides `Serialize` implementations for many Rust primitive and
/// standard library types. The complete list is [here][crate::ser]. All of
/// these can be serialized using Serde out of the box.
///
/// Additionally, Serde provides a procedural macro called [`serde_derive`] to
/// automatically generate `Serialize` implementations for structs and enums in
/// your program. See the [derive section of the manual] for how to use this.
///
/// In rare cases it may be necessary to implement `Serialize` manually for some
/// type in your program. See the [Implementing `Serialize`] section of the
/// manual for more about this.
///
/// Third-party crates may provide `Serialize` implementations for types that
/// they expose. For example the [`linked-hash-map`] crate provides a
/// [`LinkedHashMap<K, V>`] type that is serializable by Serde because the crate
/// provides an implementation of `Serialize` for it.
///
/// [Implementing `Serialize`]: https://serde.rs/impl-serialize.html
/// [`LinkedHashMap<K, V>`]: https://docs.rs/linked-hash-map/*/linked_hash_map/struct.LinkedHashMap.html
/// [`linked-hash-map`]: https://crates.io/crates/linked-hash-map
/// [`serde_derive`]: https://crates.io/crates/serde_derive
/// [derive section of the manual]: https://serde.rs/derive.html
#[cfg_attr(
    not(no_diagnostic_namespace),
    diagnostic::on_unimplemented(
        // Prevents `serde_core::ser::Serialize` appearing in the error message
        // in projects with no direct dependency on serde_core.
        message = "the trait bound `{Self}: serde::Serialize` is not satisfied",
        note = "for local types consider adding `#[derive(serde::Serialize)]` to your `{Self}` type",
        note = "for types from other crates check whether the crate offers a `serde` feature flag",
    )
)]
pub trait Serialize {
    /// Serialize this value into the given Serde serializer.
    ///
    /// See the [Implementing `Serialize`] section of the manual for more
    /// information about how to implement this method.
    ///
    /// ```edition2021
    /// use serde::ser::{Serialize, SerializeStruct, Serializer};
    ///
    /// struct Person {
    ///     name: String,
    ///     age: u8,
    ///     phones: Vec<String>,
    /// }
    ///
    /// // This is what #[derive(Serialize)] would generate.
    /// impl Serialize for Person {
    ///     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    ///     where
    ///         S: Serializer,
    ///     {
    ///         let mut s = serializer.serialize_struct("Person", 3)?;
    ///         s.serialize_field("name", &self.name)?;
    ///         s.serialize_field("age", &self.age)?;
    ///         s.serialize_field("phones", &self.phones)?;
    ///         s.end()
    ///     }
    /// }
    /// ```
    ///
    /// [Implementing `Serialize`]: https://serde.rs/impl-serialize.html
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer;
}

////////////////////////////////////////////////////////////////////////////////


/// Returned from `Serializer::serialize_seq`.
///
/// # Example use
///
/// ```edition2021
/// # use std::marker::PhantomData;
/// #
/// # struct Vec<T>(PhantomData<T>);
/// #
/// # impl<T> Vec<T> {
/// #     fn len(&self) -> usize {
/// #         unimplemented!()
/// #     }
/// # }
/// #
/// # impl<'a, T> IntoIterator for &'a Vec<T> {
/// #     type Item = &'a T;
/// #     type IntoIter = Box<dyn Iterator<Item = &'a T>>;
/// #     fn into_iter(self) -> Self::IntoIter {
/// #         unimplemented!()
/// #     }
/// # }
/// #
/// use serde::ser::{Serialize, SerializeSeq, Serializer};
///
/// impl<T> Serialize for Vec<T>
/// where
///     T: Serialize,
/// {
///     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
///     where
///         S: Serializer,
///     {
///         let mut seq = serializer.serialize_seq(Some(self.len()))?;
///         for element in self {
///             seq.serialize_element(element)?;
///         }
///         seq.end()
///     }
/// }
/// ```
///
/// # Example implementation
///
/// The [example data format] presented on the website demonstrates an
/// implementation of `SerializeSeq` for a basic JSON data format.
///
/// [example data format]: https://serde.rs/data-format.html
#[cfg_attr(
    not(no_diagnostic_namespace),
    diagnostic::on_unimplemented(
        message = "the trait bound `{Self}: serde::ser::SerializeSeq` is not satisfied",
    )
)]
pub trait SerializeSeq {
    /// Must match the `Ok` type of our `Serializer`.
    type Ok;

    /// Must match the `Error` type of our `Serializer`.
    type Error: Error;

    /// Serialize a sequence element.
    fn serialize_element<T>(&mut self, value: &T) -> Result<(), Self::Error>
    where
        T: ?Sized + Serialize;

    /// Finish serializing a sequence.
    fn end(self) -> Result<Self::Ok, Self::Error>;
}

/// Returned from `Serializer::serialize_tuple`.
///
/// # Example use
///
/// ```edition2021
/// use serde::ser::{Serialize, SerializeTuple, Serializer};
///
/// # mod fool {
/// #     trait Serialize {}
/// impl<A, B, C> Serialize for (A, B, C)
/// #     {}
/// # }
/// #
/// # struct Tuple3<A, B, C>(A, B, C);
/// #
/// # impl<A, B, C> Serialize for Tuple3<A, B, C>
/// where
///     A: Serialize,
///     B: Serialize,
///     C: Serialize,
/// {
///     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
///     where
///         S: Serializer,
///     {
///         let mut tup = serializer.serialize_tuple(3)?;
///         tup.serialize_element(&self.0)?;
///         tup.serialize_element(&self.1)?;
///         tup.serialize_element(&self.2)?;
///         tup.end()
///     }
/// }
/// ```
///
/// ```edition2021
/// # use std::marker::PhantomData;
/// #
/// # struct Array<T>(PhantomData<T>);
/// #
/// # impl<T> Array<T> {
/// #     fn len(&self) -> usize {
/// #         unimplemented!()
/// #     }
/// # }
/// #
/// # impl<'a, T> IntoIterator for &'a Array<T> {
/// #     type Item = &'a T;
/// #     type IntoIter = Box<dyn Iterator<Item = &'a T>>;
/// #     fn into_iter(self) -> Self::IntoIter {
/// #         unimplemented!()
/// #     }
/// # }
/// #
/// use serde::ser::{Serialize, SerializeTuple, Serializer};
///
/// # mod fool {
/// #     trait Serialize {}
/// impl<T> Serialize for [T; 16]
/// #     {}
/// # }
/// #
/// # impl<T> Serialize for Array<T>
/// where
///     T: Serialize,
/// {
///     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
///     where
///         S: Serializer,
///     {
///         let mut seq = serializer.serialize_tuple(16)?;
///         for element in self {
///             seq.serialize_element(element)?;
///         }
///         seq.end()
///     }
/// }
/// ```
///
/// # Example implementation
///
/// The [example data format] presented on the website demonstrates an
/// implementation of `SerializeTuple` for a basic JSON data format.
///
/// [example data format]: https://serde.rs/data-format.html
#[cfg_attr(
    not(no_diagnostic_namespace),
    diagnostic::on_unimplemented(
        message = "the trait bound `{Self}: serde::ser::SerializeTuple` is not satisfied",
    )
)]
pub trait SerializeTuple {
    /// Must match the `Ok` type of our `Serializer`.
    type Ok;

    /// Must match the `Error` type of our `Serializer`.
    type Error: Error;

    /// Serialize a tuple element.
    fn serialize_element<T>(&mut self, value: &T) -> Result<(), Self::Error>
    where
        T: ?Sized + Serialize;

    /// Finish serializing a tuple.
    fn end(self) -> Result<Self::Ok, Self::Error>;
}

/// Returned from `Serializer::serialize_tuple_struct`.
///
/// # Example use
///
/// ```edition2021
/// use serde::ser::{Serialize, SerializeTupleStruct, Serializer};
///
/// struct Rgb(u8, u8, u8);
///
/// impl Serialize for Rgb {
///     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
///     where
///         S: Serializer,
///     {
///         let mut ts = serializer.serialize_tuple_struct("Rgb", 3)?;
///         ts.serialize_field(&self.0)?;
///         ts.serialize_field(&self.1)?;
///         ts.serialize_field(&self.2)?;
///         ts.end()
///     }
/// }
/// ```
///
/// # Example implementation
///
/// The [example data format] presented on the website demonstrates an
/// implementation of `SerializeTupleStruct` for a basic JSON data format.
///
/// [example data format]: https://serde.rs/data-format.html
#[cfg_attr(
    not(no_diagnostic_namespace),
    diagnostic::on_unimplemented(
        message = "the trait bound `{Self}: serde::ser::SerializeTupleStruct` is not satisfied",
    )
)]
pub trait SerializeTupleStruct {
    /// Must match the `Ok` type of our `Serializer`.
    type Ok;

    /// Must match the `Error` type of our `Serializer`.
    type Error: Error;

    /// Serialize a tuple struct field.
    fn serialize_field<T>(&mut self, value: &T) -> Result<(), Self::Error>
    where
        T: ?Sized + Serialize;

    /// Finish serializing a tuple struct.
    fn end(self) -> Result<Self::Ok, Self::Error>;
}

/// Returned from `Serializer::serialize_tuple_variant`.
///
/// # Example use
///
/// ```edition2021
/// use serde::ser::{Serialize, SerializeTupleVariant, Serializer};
///
/// enum E {
///     T(u8, u8),
///     U(String, u32, u32),
/// }
///
/// impl Serialize for E {
///     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
///     where
///         S: Serializer,
///     {
///         match *self {
///             E::T(ref a, ref b) => {
///                 let mut tv = serializer.serialize_tuple_variant("E", 0, "T", 2)?;
///                 tv.serialize_field(a)?;
///                 tv.serialize_field(b)?;
///                 tv.end()
///             }
///             E::U(ref a, ref b, ref c) => {
///                 let mut tv = serializer.serialize_tuple_variant("E", 1, "U", 3)?;
///                 tv.serialize_field(a)?;
///                 tv.serialize_field(b)?;
///                 tv.serialize_field(c)?;
///                 tv.end()
///             }
///         }
///     }
/// }
/// ```
///
/// # Example implementation
///
/// The [example data format] presented on the website demonstrates an
/// implementation of `SerializeTupleVariant` for a basic JSON data format.
///
/// [example data format]: https://serde.rs/data-format.html
#[cfg_attr(
    not(no_diagnostic_namespace),
    diagnostic::on_unimplemented(
        message = "the trait bound `{Self}: serde::ser::SerializeTupleVariant` is not satisfied",
    )
)]
pub trait SerializeTupleVariant {
    /// Must match the `Ok` type of our `Serializer`.
    type Ok;

    /// Must match the `Error` type of our `Serializer`.
    type Error: Error;

    /// Serialize a tuple variant field.
    fn serialize_field<T>(&mut self, value: &T) -> Result<(), Self::Error>
    where
        T: ?Sized + Serialize;

    /// Finish serializing a tuple variant.
    fn end(self) -> Result<Self::Ok, Self::Error>;
}

/// Returned from `Serializer::serialize_map`.
///
/// # Example use
///
/// ```edition2021
/// # use std::marker::PhantomData;
/// #
/// # struct HashMap<K, V>(PhantomData<K>, PhantomData<V>);
/// #
/// # impl<K, V> HashMap<K, V> {
/// #     fn len(&self) -> usize {
/// #         unimplemented!()
/// #     }
/// # }
/// #
/// # impl<'a, K, V> IntoIterator for &'a HashMap<K, V> {
/// #     type Item = (&'a K, &'a V);
/// #     type IntoIter = Box<dyn Iterator<Item = (&'a K, &'a V)>>;
/// #
/// #     fn into_iter(self) -> Self::IntoIter {
/// #         unimplemented!()
/// #     }
/// # }
/// #
/// use serde::ser::{Serialize, SerializeMap, Serializer};
///
/// impl<K, V> Serialize for HashMap<K, V>
/// where
///     K: Serialize,
///     V: Serialize,
/// {
///     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
///     where
///         S: Serializer,
///     {
///         let mut map = serializer.serialize_map(Some(self.len()))?;
///         for (k, v) in self {
///             map.serialize_entry(k, v)?;
///         }
///         map.end()
///     }
/// }
/// ```
///
/// # Example implementation
///
/// The [example data format] presented on the website demonstrates an
/// implementation of `SerializeMap` for a basic JSON data format.
///
/// [example data format]: https://serde.rs/data-format.html
#[cfg_attr(
    not(no_diagnostic_namespace),
    diagnostic::on_unimplemented(
        message = "the trait bound `{Self}: serde::ser::SerializeMap` is not satisfied",
    )
)]
pub trait SerializeMap {
    /// Must match the `Ok` type of our `Serializer`.
    type Ok;

    /// Must match the `Error` type of our `Serializer`.
    type Error: Error;

    /// Serialize a map key.
    ///
    /// If possible, `Serialize` implementations are encouraged to use
    /// `serialize_entry` instead as it may be implemented more efficiently in
    /// some formats compared to a pair of calls to `serialize_key` and
    /// `serialize_value`.
    fn serialize_key<T>(&mut self, key: &T) -> Result<(), Self::Error>
    where
        T: ?Sized + Serialize;

    /// Serialize a map value.
    ///
    /// # Panics
    ///
    /// Calling `serialize_value` before `serialize_key` is incorrect and is
    /// allowed to panic or produce bogus results.
    fn serialize_value<T>(&mut self, value: &T) -> Result<(), Self::Error>
    where
        T: ?Sized + Serialize;

    /// Serialize a map entry consisting of a key and a value.
    ///
    /// Some [`Serialize`] types are not able to hold a key and value in memory
    /// at the same time so `SerializeMap` implementations are required to
    /// support [`serialize_key`] and [`serialize_value`] individually. The
    /// `serialize_entry` method allows serializers to optimize for the case
    /// where key and value are both available. [`Serialize`] implementations
    /// are encouraged to use `serialize_entry` if possible.
    ///
    /// The default implementation delegates to [`serialize_key`] and
    /// [`serialize_value`]. This is appropriate for serializers that do not
    /// care about performance or are not able to optimize `serialize_entry` any
    /// better than this.
    ///
    /// [`Serialize`]: crate::Serialize
    /// [`serialize_key`]: Self::serialize_key
    /// [`serialize_value`]: Self::serialize_value
    fn serialize_entry<K, V>(&mut self, key: &K, value: &V) -> Result<(), Self::Error>
    where
        K: ?Sized + Serialize,
        V: ?Sized + Serialize,
    {
        tri!(self.serialize_key(key));
        self.serialize_value(value)
    }

    /// Finish serializing a map.
    fn end(self) -> Result<Self::Ok, Self::Error>;
}

/// Returned from `Serializer::serialize_struct`.
///
/// # Example use
///
/// ```edition2021
/// use serde::ser::{Serialize, SerializeStruct, Serializer};
///
/// struct Rgb {
///     r: u8,
///     g: u8,
///     b: u8,
/// }
///
/// impl Serialize for Rgb {
///     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
///     where
///         S: Serializer,
///     {
///         let mut rgb = serializer.serialize_struct("Rgb", 3)?;
///         rgb.serialize_field("r", &self.r)?;
///         rgb.serialize_field("g", &self.g)?;
///         rgb.serialize_field("b", &self.b)?;
///         rgb.end()
///     }
/// }
/// ```
///
/// # Example implementation
///
/// The [example data format] presented on the website demonstrates an
/// implementation of `SerializeStruct` for a basic JSON data format.
///
/// [example data format]: https://serde.rs/data-format.html
#[cfg_attr(
    not(no_diagnostic_namespace),
    diagnostic::on_unimplemented(
        message = "the trait bound `{Self}: serde::ser::SerializeStruct` is not satisfied",
    )
)]
pub trait SerializeStruct {
    /// Must match the `Ok` type of our `Serializer`.
    type Ok;

    /// Must match the `Error` type of our `Serializer`.
    type Error: Error;

    /// Serialize a struct field.
    fn serialize_field<T>(&mut self, key: &'static str, value: &T) -> Result<(), Self::Error>
    where
        T: ?Sized + Serialize;

    /// Indicate that a struct field has been skipped.
    ///
    /// The default implementation does nothing.
    #[inline]
    fn skip_field(&mut self, key: &'static str) -> Result<(), Self::Error> {
        let _ = key;
        Ok(())
    }

    /// Finish serializing a struct.
    fn end(self) -> Result<Self::Ok, Self::Error>;
}

/// Returned from `Serializer::serialize_struct_variant`.
///
/// # Example use
///
/// ```edition2021
/// use serde::ser::{Serialize, SerializeStructVariant, Serializer};
///
/// enum E {
///     S { r: u8, g: u8, b: u8 },
/// }
///
/// impl Serialize for E {
///     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
///     where
///         S: Serializer,
///     {
///         match *self {
///             E::S {
///                 ref r,
///                 ref g,
///                 ref b,
///             } => {
///                 let mut sv = serializer.serialize_struct_variant("E", 0, "S", 3)?;
///                 sv.serialize_field("r", r)?;
///                 sv.serialize_field("g", g)?;
///                 sv.serialize_field("b", b)?;
///                 sv.end()
///             }
///         }
///     }
/// }
/// ```
///
/// # Example implementation
///
/// The [example data format] presented on the website demonstrates an
/// implementation of `SerializeStructVariant` for a basic JSON data format.
///
/// [example data format]: https://serde.rs/data-format.html
#[cfg_attr(
    not(no_diagnostic_namespace),
    diagnostic::on_unimplemented(
        message = "the trait bound `{Self}: serde::ser::SerializeStructVariant` is not satisfied",
    )
)]
pub trait SerializeStructVariant {
    /// Must match the `Ok` type of our `Serializer`.
    type Ok;

    /// Must match the `Error` type of our `Serializer`.
    type Error: Error;

    /// Serialize a struct variant field.
    fn serialize_field<T>(&mut self, key: &'static str, value: &T) -> Result<(), Self::Error>
    where
        T: ?Sized + Serialize;

    /// Indicate that a struct variant field has been skipped.
    ///
    /// The default implementation does nothing.
    #[inline]
    fn skip_field(&mut self, key: &'static str) -> Result<(), Self::Error> {
        let _ = key;
        Ok(())
    }

    /// Finish serializing a struct variant.
    fn end(self) -> Result<Self::Ok, Self::Error>;
}

fn iterator_len_hint<I>(iter: &I) -> Option<usize>
where
    I: Iterator,
{
    match iter.size_hint() {
        (lo, Some(hi)) if lo == hi => Some(lo),
        _ => None,
    }
}

