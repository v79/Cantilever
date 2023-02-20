<script>
    import LoginButton from './loginButton.svelte';
    import {activeStore} from '../stores/appStatusStore.svelte';
    import {Navbar, NavBrand, NavLi, NavUl} from 'flowbite-svelte';
    import CToast from './customized/cToast.svelte';
</script>

<Navbar
	color="none"
	navClass="bg-slate-600 text-gray-200 shadow-lg py-4 px-4"
	fluid={false}
	navDivClass="mx-auto flex flex-wrap justify-between items-center ">
	<NavBrand href="/">
		<img
			src="https://flowbite.com/docs/images/logo.svg"
			class="mr-3 h-6 sm:h-9"
			alt="Flowbite Logo" />
		<span class="self-center whitespace-nowrap text-xl font-semibold dark:text-white">
			Cantilever
		</span>
	</NavBrand>
	<NavUl nonActiveClass="text-gray-200">
		<NavLi href="/" active={false} nonActiveClass="text-gray-200">Home</NavLi>
		<NavLi href="/" active={false} nonActiveClass="text-gray-200">Other</NavLi>
		<NavLi href="/" active={false} nonActiveClass="text-gray-200">Menu</NavLi>
	</NavUl>
	<div class="flex-grow items-center justify-between">
		<p>
			<strong>Current file:</strong>
			{$activeStore.activeFile} <strong>Is new:</strong>
			{$activeStore.isNewFile} <strong>Has changed: </strong>{$activeStore.hasChanged}
			<strong>Is valid: </strong>{$activeStore.isValid}
		</p>
	</div>
	<div class="flex flex-wrap items-center justify-end">
		<LoginButton />
	</div>
</Navbar>

<CToast />
