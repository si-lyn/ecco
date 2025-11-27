#[derive(Subcommand, Debug, Clone)]
#[command(rename_all = "lowercase")]
pub enum Commands {
    /// Change the password for an existing user
    #[cfg(feature = "change")]
    Change {
        /// The username whose password will be changed
        #[arg(short, long)]
        user: String,
        /// The new password for the user
        #[arg(short, long)]
        password: String,
    },
    /// Get all users
    #[cfg(feature = "get_all")]
    GetAll,
}