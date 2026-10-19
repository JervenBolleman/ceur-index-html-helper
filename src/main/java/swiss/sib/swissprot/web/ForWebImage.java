package swiss.sib.swissprot.web;

import java.util.function.BooleanSupplier;

/**
 * Enables substitutions only in the WebAssembly build, which passes
 * -Dceur.web-image=true to native-image (see the wasm profile in the pom).
 */
final class ForWebImage implements BooleanSupplier {
	@Override
	public boolean getAsBoolean() {
		return Boolean.getBoolean("ceur.web-image");
	}
}
