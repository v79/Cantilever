<script lang="ts">
	export let name: string;
	export let value: boolean;
	export let label: string;
	export let readonly = false;
	export let onChange = (e: Event) => {};
	export let classes = 'ms-2 text-sm font-medium text-gray-900 dark:text-gray-300';

	export let onInput: (e: Event) => void = (e: Event) => {
		const { target } = e;
		if (target) {
			value = (target as HTMLInputElement).checked;
		}
	};

	let classesToApply: string = '';
	$: if (readonly) {
		classesToApply = classes + ' bg-slate-200 text-slate-500 focus:border-gray-300';
	} else {
		classesToApply = classes + ' focus:border-indigo-500 focus:ring-indigo-500';
	}
</script>

<label for={name} class="block text-sm font-medium text-slate-200">{label}</label>
<input
	type="checkbox"
	checked={value}
	{name}
	id={name}
	disabled={readonly || null}
	on:change={onChange}
	on:input={onInput}
	class={classesToApply} />
