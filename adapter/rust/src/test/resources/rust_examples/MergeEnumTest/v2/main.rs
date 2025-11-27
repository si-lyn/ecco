#[derive(Subcommand, Debug, Clone)]
#[command(rename_all = "lowercase")]
pub enum Commands {
    /// Create a new user with a password
    #[cfg(feature = "create")]
    Create {
        /// The username for the new user
        #[arg(short, long)]
        user: String,
        /// The password for the user
        #[arg(short, long)]
        password: String,
    },

    /// Retrieve the password for a user
    #[cfg(feature = "get")]
    Get {
        /// The username whose password will be retrieved
        #[arg(short, long)]
        user: String,
    },
}