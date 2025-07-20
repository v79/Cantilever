<script lang="ts">
	import {
		PUBLIC_COGNITO_CALLBACK_URL,
		PUBLIC_COGNITO_CLIENT_ID,
		PUBLIC_COGNITO_DOMAIN
	} from '$env/static/public';
	import { jwtDecode, type JwtPayload } from 'jwt-decode';
	import { CLEAR_USER, userStore } from '$lib/stores/userStore.svelte';
	import { Avatar } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { User } from '$lib/models/user.svelte';
	import { Icon, Login, Logout } from 'svelte-google-materialdesign-icons';

	let authToken: string | undefined;
	let tokenPayload: JwtPayload;

	const appClientId = PUBLIC_COGNITO_CLIENT_ID;
	const cognitoDomain = PUBLIC_COGNITO_DOMAIN;
	const loginUrl =
		cognitoDomain +
		'/oauth2/authorize?response_type=token&client_id=' +
		appClientId +
		'&redirect_uri=' +
		PUBLIC_COGNITO_CALLBACK_URL +
		'&scope=openid+profile';

	const logoutUrl =
		cognitoDomain +
		'/logout?client_id=' +
		appClientId +
		'&logout_uri=' +
		PUBLIC_COGNITO_CALLBACK_URL;

	onMount(async () => {
		authToken = extractIdToken();
		if (authToken) {
			tokenPayload = jwtDecode<JwtPayload>(authToken);
			if (tokenPayload) {
				userStore.set(
					new User(
						tokenPayload.name,
						tokenPayload.sub,
						tokenPayload.email,
						tokenPayload.auth_time,
						authToken
					)
				);
			}
		}
	});

	function extractIdToken(): string | undefined {
		if (window.location.hash) {
			let hash = window.location.hash.substring(1);
			let regex = /id_token=([^&]*)/;
			let match = hash.match(regex);
			if (match) {
				return match[1];
			}
		}
		return undefined;
	}

	function initLogin() {
		if (authToken) {
			return;
		} else {
			window.location.assign(loginUrl);
		}
	}

	function initLogout() {
		userStore.set(CLEAR_USER);
		// TODO: clear other stores
		window.location.assign(logoutUrl);
	}

	const unsubscribe = userStore.subscribe((value) => {
		if (value) {
			authToken = value.token;
		}
	});
</script>

{#if $userStore.token}
	<button
		type="button"
		class="btn btn-sm variant-ghost-secondary"
		on:click={initLogout}
		title="Logout">
		<Icon icon={Logout} />Logout</button>
	<Avatar width="w-8" initials="LD" alt={$userStore.name} />
{:else}
	<button
		type="button"
		class="btn btn-sm variant-ghost-secondary"
		title="Login"
		on:click={initLogin}>
		<Icon icon={Login} />Login</button>
{/if}
