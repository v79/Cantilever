<script lang="ts" context="module">
	import { writable } from 'svelte/store';
	import type { User } from '../models/user.svelte';

	export const CLEAR_USER = { sub: '', email: '', auth_time: 0, name: '', token: '' }
	export const userStore = writable<User>(CLEAR_USER);
	
</script>
