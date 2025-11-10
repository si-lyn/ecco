trait Logger {
    fn log(&self, message: &str);
}
#[cfg(unix)]
struct UnixLogger;
#[cfg(unix)]
impl Logger for UnixLogger {
    fn log(&self, message: &str) {
        println!("Unix log: {}", message);
    }
}
#[cfg(windows)]
struct WindowsLogger;
#[cfg(windows)]
impl Logger for WindowsLogger {
    fn log(&self, message: &str) {
        println!("Windows log: {}", message);
    }
}
fn perform_logging<L: Logger>(logger: L) {
    logger.log("This is a platform-specific log message.");
}
fn main() {
    #[cfg(unix)]
    {
        let logger = UnixLogger;
        perform_logging(logger);
    }

    #[cfg(windows)]
    {
        let logger = WindowsLogger;
        perform_logging(logger);
    }
}
