#[cfg(feature = "hello")]
fn hello(str: &str){
    println!("Hello, {} from v1", str);
}

fn main(){
    #[cfg(feature = "hello")]
    hello("world");
}