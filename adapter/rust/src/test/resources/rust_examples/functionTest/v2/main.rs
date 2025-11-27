#[cfg(feature = "farewell")]
fn farewell(str: &str){
    println!("Farewell, {} from v2", str);
}

fn main(){
    #[cfg(feature = "farewell")]
    farewell("world");
}
