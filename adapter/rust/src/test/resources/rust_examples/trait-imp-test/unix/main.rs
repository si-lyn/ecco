// Define a trait for logging
trait Logger {
    fn log(&self, message: &str);
}

// Unix-specific implementation
#[cfg(unix)]
struct UnixLogger;

#[cfg(unix)]
impl Logger for UnixLogger {
    fn log(&self, message: &str) {
        println!("Unix log: {}", message);
    }
}

// Generic function using the trait
fn perform_logging<L: Logger>(logger: L) {
    logger.log("This is a platform-specific log message.");
}

fn main() {
    #[cfg(unix)]
    {
        let logger = UnixLogger;
        perform_logging(logger);
    }
}
