// Module
mod my_module {
    pub fn module_function() {
        println!("Function in module");
    }
}

// Static mut
static mut COUNTER: i32 = 0;

// External crate
extern crate std;

// Use declaration
use std::collections::HashMap;

// Type alias
type StringMap = HashMap<String, String>;

// Struct
struct Point {
    x: i32,
    y: i32,
}

// Tuple struct
struct Color(u8, u8, u8);

// Unit struct
struct Marker;

// Enum
enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    ChangeColor(u8, u8, u8),
}

// Union
union MyUnion {
    i: i32,
    f: f32,
}

// Constant
const MAX_POINTS: u32 = 100_000;

// Static
static LANGUAGE: &str = "Rust";

// Trait
trait Drawable {
    fn draw(&self);
}

// Trait implementation
impl Drawable for Point {
    fn draw(&self) {
        println!("Drawing point at ({}, {})", self.x, self.y);
    }
}

// Inherent implementation
impl Point {
    fn new(x: i32, y: i32) -> Self {
        Point { x, y }
    }
}

// Function
fn add(a: i32, b: i32) -> i32 {
    a + b
}

// Generic function
fn generic_function<T: std::fmt::Display>(item: T) {
    println!("Generic: {}", item);
}

// Macro definition
macro_rules! say_hello {
    ($name:expr) => {
        println!("Hello, {}!", $name);
    };
}

// Associated type in trait
trait Container {
    type Item;
    fn get(&self) -> &Self::Item;
}

// Main function
fn main() {
    let point = Point::new(10, 20);
    point.draw();

    let _color = Color(255, 0, 0);
    let _marker = Marker;

    let _msg = Message::Write(String::from("test"));

    println!("Result: {}", add(5, 3));
    generic_function(42);

    let mut map: StringMap = HashMap::new();
    map.insert(String::from("key"), String::from("value"));

    say_hello!("Rust");

    println!("Constant: {}", MAX_POINTS);
    println!("Static: {}", LANGUAGE);

    println!("Counter: {}", unsafe { COUNTER } );
}
