<script lang="ts">
	import TextInput from '$lib/forms/textInput.svelte';
	import type { ImgRes } from '$lib/models/project';

	export let index: number;
	export let key: string;
	export let res: ImgRes;

	$: resW = res.w?.toString() ?? '';
	$: resH = res.h?.toString() ?? '';
</script>

<div class="mt-1 grid grid-cols-7 gap-6 w-6/12">
	<div class="col-span-3 sm:col-span-3 lg:col-span-3">
		<TextInput required value={key} name="imageres-{index}" label="Name" classes="text-sm" />
	</div>
	<div class="col-span-2 sm:col-span-2 lg:col-span-2">
		<TextInput
			type="number"
			value={resW}
			name="imageres-width-{index}"
			label="Width"
			classes="text-sm" />
	</div>
	<div class="col-span-2 sm:col-span-2 lg:col-span-2">
		<TextInput
			type="number"
			value={resH}
			name="imageres-height-{index}"
			label="Height"
			classes="text-sm" />
	</div>
</div>
