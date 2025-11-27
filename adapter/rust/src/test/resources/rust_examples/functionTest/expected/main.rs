#[cfg(feature = "farewell")]
fn farewell(str: &str) {
    println!("Farewell, {} from v2", str);
}
#[cfg(feature = "hello")]
fn hello(str: &str) {
    println!("Hello, {} from v1", str);
}
fn main() {
    #[cfg(feature = "farewell")]
    farewell("world");
    #[cfg(feature = "hello")]
    hello("world");
}
