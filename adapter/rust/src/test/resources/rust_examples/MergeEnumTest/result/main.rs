#[derive(Subcommand, Debug, Clone)]
#[command(rename_all = "lowercase")]
pub enum Commands {
    /// Create a new user with a password
    Create {
        /// The username for the new user
        #[arg(short, long)]
        user: String,
        /// The password for the user
        #[arg(short, long)]
        password: String,
    },
    /// Retrieve the password for a user
    Get {
        /// The username whose password will be retrieved
        #[arg(short, long)]
        user: String,
    },
    /// Change the password for an existing user
    Change {
        /// The username whose password will be changed
        #[arg(short, long)]
        user: String,
        /// The new password for the user
        #[arg(short, long)]
        password: String,
    },
    /// Get all users
    GetAll,
}
