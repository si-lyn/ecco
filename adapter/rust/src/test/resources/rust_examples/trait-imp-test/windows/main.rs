// Define a trait for logging
trait Logger {
    fn log(&self, message: &str);
}

// Windows-specific implementation
#[cfg(windows)]
struct WindowsLogger;

#[cfg(windows)]
impl Logger for WindowsLogger {
    fn log(&self, message: &str) {
        println!("Windows log: {}", message);
    }
}

// Generic function using the trait
fn perform_logging<L: Logger>(logger: L) {
    logger.log("This is a platform-specific log message.");
}

fn main() {
    #[cfg(windows)]
    {
        let logger = WindowsLogger;
        perform_logging(logger);
    }
}
