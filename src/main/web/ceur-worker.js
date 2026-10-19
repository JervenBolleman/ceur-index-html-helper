// Runs the WebAssembly build of make-index off the main thread.
// Receives {files: [{path, bytes}], args: [...]} and replies with the generated files.
const forward = (level) => (...parts) => postMessage({ type: "log", level, text: parts.map(String).join(" ") });
console.log = forward("log");
console.info = forward("log");
console.warn = forward("warn");
console.error = forward("error");

importScripts("ceur-index-html-helper.js");

onmessage = async (event) => {
	const { files, args } = event.data;
	self.ceurInput = { files };
	self.ceurOutput = { files: [], exitCode: -1 };
	const config = new GraalVM.Config();
	config.wasm_path = "ceur-index-html-helper.js.wasm";
	try {
		await GraalVM.run(args, config);
		const output = self.ceurOutput;
		postMessage({ type: "done", output }, output.files.map((f) => f.bytes.buffer));
	} catch (e) {
		postMessage({ type: "failed", text: String((e && e.stack) || e) });
	} finally {
		self.ceurInput = undefined;
		self.ceurOutput = undefined;
	}
};
