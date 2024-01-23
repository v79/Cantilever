<script lang="ts">
	export let title: string;
	export let count: number;
	export let srcKey: string;

	let disabledColor = '';
	$: {
		if (count === 0) {
			disabledColor = 'text-surface-600';
		} else {
			disabledColor = '';
		}
	}
</script>

<div class="flex flex-row justify-between w-full">
	<span class="text-left font-bold {disabledColor}">{title}</span>
	<span class="text-secondary-200 text-right text-sm {disabledColor}">{count}</span>
</div>
<span class="text-sm {disabledColor}">{srcKey}</span>
