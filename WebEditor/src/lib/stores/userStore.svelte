<script lang="ts" context="module">
	import { writable } from 'svelte/store';
	import type { User } from '$lib/models/user.svelte';

	export const CLEAR_USER = { sub: '', email: '', auth_time: 0, name: '', token: '', isLoggedIn: () => false }
	export const userStore = writable<User>(CLEAR_USER);
	
</script>
