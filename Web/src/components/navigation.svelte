<script>
    import LoginButton from './loginButton.svelte';
    import {activeStore} from '../stores/appStatusStore.svelte';
    import {Navbar, NavBrand, NavLi, NavUl} from 'flowbite-svelte';
</script>

<Navbar
	navClass="bg-slate-800 text-gray-500 shadow-lg py-4 px-4"
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
	<NavUl>
		<NavLi href="/" active={false}>Home</NavLi>
		<NavLi href="/" active={false}>Other</NavLi>
		<NavLi href="/" active={false}>Menu</NavLi>
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
