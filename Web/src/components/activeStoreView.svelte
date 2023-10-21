<script lang="ts">
	import { activeStore } from '../stores/appStatusStore.svelte';
	const none: string = 'none';
</script>

<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-200">Current file</h3>
<div>
	<dl class="px-4 py-4 text-slate-200">
		<dt class="font-bold">File</dt>
		<dl class="px-8 text-slate-200">{$activeStore.activeFile ? $activeStore.activeFile : none}</dl>
		<dt class="font-bold">New?</dt>
		<dl class="px-8 text-slate-200">{$activeStore.isNewFile}</dl>
		<dt class="font-bold">Has changed?</dt>
		<dl class="px-8 text-slate-200">{$activeStore.hasChanged}</dl>
		<dt class="font-bold">Valid?</dt>
		<dl class="px-8 text-slate-200">{$activeStore.isValid}</dl>
		<dt class="font-bold">Slug/URL</dt>
		<dl class="px-8 text-slate-200">{$activeStore.newSlug}</dl>
		<dt class="font-bold">File type</dt>
		<dl class="px-8 text-slate-200">{$activeStore.fileType}</dl>
		<dt class="font-bold">Folder</dt>
		<dl class="px-8 text-slate-200">{$activeStore.folder ? $activeStore.folder.srcKey : none}</dl>
	</dl>
</div>
