use crate::lib::*;


// The generated code calls this like:
//
//     let value = &_serde::__private::from_utf8_lossy(bytes);
//     Err(_serde::de::Error::unknown_variant(value, VARIANTS))
//
// so it is okay for the return type to be different from the std case as long
// as the above works.

