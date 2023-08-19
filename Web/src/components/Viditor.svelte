<script lang="ts">
	// English docs for Vditor here:
	// https://github.com/Vanessa219/vditor/blob/master/README_en_US.md?utm_source=ld246.com
	// weird REPL gotchas: the toolbar icons will disappear from the Repl Result when you edit this file, you will need to reload (F5)
	// import { v4 as uuidv4 } from 'uuid'; // not liked by Repl
	import Vditor from 'vditor';
	export let body = '';
	export let id = Math.random().toString(); // uuidv4();

	export let onChange = (newBody: string) => {};

	let vditor: Vditor = undefined;

	let initialize = () => {
		// onMount because it relies on an id, you can't pass an element to the fn
		// should autogenerate an id to avoid conflicts with multiple instances

		vditor = new Vditor(id, {
			height: 400,
			// i18n: "en_US",
			lang: 'en_US',
			value: body,
			counter: { enable: true },
			toolbar: [
				// "emoji",
				'headings',
				'bold',
				'italic',
				'strike',
				'|',
				'line',
				'quote',
				'list',
				'ordered-list',
				'|',
				// "check",
				// 'outdent',
				// 'indent',
				'code',
				'inline-code',
				//"insert-after",
				//"insert-before",
				//"code-theme",
				//"content-theme",
				//"export",
				'|',
				'undo',
				'redo',
				//"upload",
				'link',
				'table',
				//"record",
				//"edit-mode",
				//"both",
				// 'preview',
				'fullscreen',
				// 'outline',
				'devtools',
				'br'
			],
			cdn: 'https://cdn.jsdelivr.net/npm/vditor@3.8.7', //defaults to jsdlvr
			toolbarConfig: {
				pin: true
			},
			cache: {
				enable: false
			},
			after: () => {},
			input: (val) => {
				{
					onChange(val);
					body = val;
				}
			}
		});
	};

	// update parent component value
	let update = (val) => {
		if (typeof vditor != 'undefined') {
			if (body != vditor.getValue()) {
				vditor.setValue(body);
			}
		}
	};

	$: update(body);
</script>

<div {id} use:initialize />

<style>
	@import 'https://cdn.jsdelivr.net/npm/vditor@3.8.7/dist/index.css';
	@import 'https://cdn.jsdelivr.net/npm/vditor@3.8.7/dist/css/content-theme/light.css';
	#vditor {
		margin-top: 32px;
	}
</style>
