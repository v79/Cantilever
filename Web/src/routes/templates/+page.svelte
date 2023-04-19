<script lang="ts">
    import {afterNavigate} from '$app/navigation';
    import SpinnerWrapper from '../../components/utilities/spinnerWrapper.svelte';
    import {activeStore} from '../../stores/appStatusStore.svelte';

    afterNavigate(() => {
		$activeStore.currentPage = 'Templates';
		$activeStore.activeFile = '';
	});
</script>

<div class="flex grow flex-row">
	<div class="basis-1/4 bg-slate-400">
		<h2>Template list</h2>
	</div>
	<div class="basis-1/2 bg-slate-600">
		<h2>Handlebars editor</h2>
	</div>
	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-200">Messages</h3>
		<SpinnerWrapper spinnerID="globalSpinner" />
	</div>
</div>
