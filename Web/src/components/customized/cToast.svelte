<script lang="ts">
    import {slide} from 'svelte/transition';
    import {notificationStore} from '../../stores/notificationStore.svelte';
    import {Toast} from 'flowbite-svelte';
</script>

<Toast
	transition={slide}
	bind:open={$notificationStore.shown}
	position="top-right"
	color="none"
	divClass="bg-green-200 w-full max-w-sm p-4">
	{$notificationStore.message}
</Toast>
