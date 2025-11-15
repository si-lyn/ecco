trait Logger {
    fn log(&self, message: &str);
}
#[cfg(unix)]
#[cfg(windows)]
struct UnixLogger;
struct WindowsLogger;
#[cfg(unix)]
#[cfg(windows)]
impl Logger for UnixLogger {
    impl Logger for WindowsLogger {
        fn log(&self, message: &str) {
            println!("Unix log: {}", message);
            println!("Windows log: {}", message);
        }
    }
    fn perform_logging<L: Logger>(logger: L) {
        logger.log("This is a platform-specific log message.");
    }
    fn main() {
        #[cfg(unix)]
        #[cfg(windows)]
        {
            let logger = UnixLogger;
            let logger = WindowsLogger;
            perform_logging(logger);
        }
    }
