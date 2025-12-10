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



////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////////









fn iterator_len_hint<I>(iter: &I) -> Option<usize>
where
    I: Iterator,
{
    match iter.size_hint() {
        (lo, Some(hi)) if lo == hi => Some(lo),
        _ => None,
    }
}

