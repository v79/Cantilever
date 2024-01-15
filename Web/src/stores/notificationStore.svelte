<script context="module" lang="ts">
	/** This store manages the notification display for 'Toasts'*/
	import { writable } from 'svelte/store';
	import type { Notification } from '../models/notification';

	export const notificationStore = writable<Notification>({
		message: '',
		shown: false,
		type: 'info'
	});
</script>
