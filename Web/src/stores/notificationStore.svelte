<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import type { Notification } from '../models/notification';

	export const notificationStore = writable<Notification>({
		message: '',
		shown: false,
		type: 'info'
	});
</script>
