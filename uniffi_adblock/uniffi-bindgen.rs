// This works around an issue where uniffi isn't "installed" so the binary is built locally
// See https://mozilla.github.io/uniffi-rs/tutorial/foreign_language_bindings.html#creating-the-bindgen-binary
fn main() {
    uniffi::uniffi_bindgen_main()
}
