<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  ArrowRight,
  Cpu,
  FlaskConical,
  BarChart3,
  Users,
  BookOpen,
  ExternalLink,
  Calendar
} from 'lucide-vue-next'

import { getGroupBadgeColor } from '@/utils/groupColor'
import AppBadge from '@/components/ui/AppBadge.vue'

const stats = [
  { value: '30+', label: 'Applications' },
  { value: '1,500+', label: 'Researchers' },
  { value: '65  +', label: 'Publications' },
  { value: '15+', label: 'Years of experience' },
]

// For now we fake apps (need a popular apps public route)
const topApplications = [
  {
    name: 'FreeSurfer',
    description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
    executions: 18420,
    category: 'Neuroimaging',
    version: '7.3.2',
  },
  {
    name: 'CQuest',
    description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
    executions: 18420,
    category: 'Spectroscopy',
    version: '7.3.2',
  },
  {
    name: 'LCModel',
    description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
    executions: 18420,
    category: 'Spectroscopy',
    version: '7.3.2',
  },
  {
    name: 'BraTS',
    description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
    executions: 18420,
    category: 'Neuroimaging',
    version: '7.3.2',
  },
  {
    name: 'Gate',
    description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
    executions: 18420,
    category: 'Simulation',
    version: '7.3.2',
  },
  {
    name: 'FreeSurfer',
    description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
    executions: 18420,
    category: 'Neuroimaging',
    version: '7.3.2',
  },
]

// Same for publications and team members, we should have public routes for that
const publications = [
  {
    title: '	The practical impact of numerical variability on structural MRI measures of Parkinsons disease',
    authors: 'Chatelain, Yohan and Soko-lowski, Andrzej and Sharp, Madeleine and Poline, Jean-Baptiste and Glatard, Tristan',
    journal: 'bioRxiv',
    year: 2026,
    doi: '',
    tags: ['Neuroimaging', 'Reproducibility'],
  },
  {
    title: 'Estimation of reference curves for brain atrophy and analysis of robustness to machine effects',
    authors: '	Elodie Piot, Félix Renard, Arnaud Attyé, Alexandre Krainik',
    journal: 'Scientific Reports',
    year: 2025,
    doi: '',
    tags: ['Simulation', 'Medical Physics'],
  },
  {
    title: 'Development and testing of a phantom for CT-based examination of head and neck tumours',
    authors: 'Maike Rosendahl',
    journal: 'Master Thesis',
    year: 2024,
    doi: '',
    tags: ['Federated', 'EUCAIM'],
  },
  {
    title: 'Development and characterization of modular mouse phantoms for end-to-end testing and training in radiobiology experiments',
    authors: 'Marie Wegner, Thorsten Frenzel, Dieter Krause and Elisabetta Gargioni',
    journal: 'Physics in Medicine and Biology	',
    year: 2023,
    doi: '',
    tags: ['Reproducibility', 'Grid'],
  },
]

// But for the team, its probably something that we can hardcode...
const permanentTeam = [
  {
    name: 'Sorina Pop',
    role: 'Project Manager',
    org: 'CREATIS',
    url: 'https://www.creatis.insa-lyon.fr/site7/fr/users/camarasu',
    photo: '/team/sorina_pop.jpg',
  },
  {
    name: 'Axel Bonnet',
    role: 'Main Developer',
    org: 'CREATIS',
    url: 'https://www.egi.eu/people/axel-bonnet/',
    photo: '/team/axel_bonnet.jpg',
  },
]

const fixedTermTeam = [
  {
    name: 'Guillaume Vinet',
    role: 'Research Engineer',
    org: 'CREATIS',
    url: null,
    photo: '/team/guillaume_vinet.png',
  },
  {
    name: 'Mayssa Rouissi',
    role: 'Research Engineer',
    org: 'CREATIS',
    url: null,
    photo: '/team/mayssa_rouissi.png',
  },
  {
    name: 'Hippolyte Blot',
    role: 'Apprentice',
    org: 'CREATIS',
    url: null,
    photo: '/team/hippolyte_blot.jpg',
  },
]

const formerContributors = [
  {
    name: 'Tristan Glatard',
    role: 'VIP Founder',
    org: 'Concordia University',
    url: 'https://users.encs.concordia.ca/~tglatard/',
    photo: '/team/tristan_glatard.jpg',
  },
  {
    name: 'Rafael Ferreira da Silva',
    role: 'Senior Research Scientist',
    org: 'Oak Ridge National Laboratory',
    url: 'https://rafaelsilva.com/',
    photo: '/team/rafael_silva.jpg',
  },
  {
    name: 'Gaël Vila',
    role: 'Post-Doctoral Researcher',
    org: 'ReproVIP',
    url: 'https://www.creatis.insa-lyon.fr/reprovip/',
    photo: '/team/gael_vila.jpg',
  },
  {
    name: 'Alexandre Cornier',
    role: 'Research Engineer',
    org: 'EGI-ACE & ReproVIP',
    url: 'https://www.egi.eu/project/egi-ace/',
    photo: '/team/alex_cornier.jpg',
  },
  {
    name: 'Sandesh Patil',
    role: 'Research Engineer',
    org: 'FLI-IAM',
    url: 'https://www.francelifeimaging.fr/',
    photo: '/team/sandesh_patil.jpg',
  },
  {
    name: 'Ethaniel Billon',
    role: 'Engineer',
    org: 'PEPR Chronicardio',
    url: 'https://pepr-santenum.fr/2023/11/08/chronicardio/',
    photo: '/team/ethaniel_billon.jpg',
  },
  {
    name: 'Nicolas Georges',
    role: 'Research Engineer',
    org: 'EUCAIM',
    url: 'https://www.eibir.org/projects/eucaim/',
    photo: '/team/nicolas_georges.png',
  },
  {
    name: 'Gwenaël Ambrosino-Ielpo',
    role: 'Research Engineer',
    org: 'FLI-IAM',
    url: 'https://www.francelifeimaging.fr/',
    photo: '/team/gwenael_ambrosino.jpg',
  },
]

const steeringCommittee = [
  { name: 'Olivier Beuf', role: 'Director of CREATIS laboratory' },
  { name: 'Hugues Benoit-Cattin', role: 'Deputy Director INSA Lyon, Digital' },
  { name: 'Jérôme Pansanel', role: 'Technical Director at IPHC' },
]

function formatNumber(n: number) {
  return n.toLocaleString('en-US')
}

const heroSection = ref<HTMLElement | null>(null)
const useSolidNavbar = ref(false)

function updateNavbarState() {
  const heroBottom = heroSection.value?.getBoundingClientRect().bottom ?? 0
  // Switch to solid navbar once hero is mostly out of view.
  useSolidNavbar.value = heroBottom <= 96
}

onMounted(() => {
  updateNavbarState()
  window.addEventListener('scroll', updateNavbarState, { passive: true })
  window.addEventListener('resize', updateNavbarState)
})

onUnmounted(() => {
  window.removeEventListener('scroll', updateNavbarState)
  window.removeEventListener('resize', updateNavbarState)
})
</script>

<template>
  <div class="min-h-screen bg-white font-sans">

    <!-- Navbar -->
    <header
      :class="[
        'fixed inset-x-0 top-0 z-50 border-b backdrop-blur transition-colors duration-300',
        useSolidNavbar
          ? 'border-primary-500/30 bg-linear-to-r from-primary-700/95 to-primary-900/95 shadow-lg shadow-primary-900/20'
          : 'border-white/10 bg-transparent',
      ]"
    >
      <div class="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
        <div class="flex items-center gap-3">
          <img src="/team/vip-logo-without-text.png" alt="VIP Logo" class="h-8 w-auto rounded-sm" />
          <span class="text-lg font-bold text-white">VIP Portal</span>
        </div>
        <nav class="hidden items-center gap-8 text-sm font-medium text-primary-200 md:flex">
          <a href="#about" class="transition hover:text-white">About</a>
          <a href="#applications" class="transition hover:text-white">Applications</a>
          <a href="#publications" class="transition hover:text-white">Publications</a>
          <a href="#team" class="transition hover:text-white">Team</a>
        </nav>
        <div class="flex items-center gap-3">
          <RouterLink
            to="/login"
            class="rounded-lg px-4 py-2 text-sm font-medium text-primary-100 transition hover:text-white"
          >
            Sign in
          </RouterLink>
          <RouterLink
            to="/register"
            class="rounded-lg bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm transition hover:bg-primary-50"
          >
            Sign up
          </RouterLink>
        </div>
      </div>
    </header>

    <!-- Hero (same blue than the auth layout) -->
    <section ref="heroSection" class="relative overflow-hidden bg-linear-to-br from-primary-600 via-primary-700 to-primary-900 pt-16">
      <!-- Decorative blobs -->
      <div class="pointer-events-none absolute -left-32 -top-32 h-96 w-96 rounded-full bg-white/5 blur-3xl" />
      <div class="pointer-events-none absolute -bottom-20 right-0 h-80 w-80 rounded-full bg-primary-400/20 blur-3xl" />
      <div class="pointer-events-none absolute right-1/4 top-1/3 h-64 w-64 rounded-full bg-white/5 blur-2xl" />

      <div class="relative mx-auto max-w-7xl px-6 py-24 sm:py-32 lg:py-40">
        <div class="grid items-center gap-16 lg:grid-cols-2">
          <!-- Left -->
          <div class="text-white">
            <h1 class="mt-4 text-5xl font-extrabold leading-tight tracking-tight sm:text-6xl">
              Virtual<br />
              <span class="bg-linear-to-r from-white to-primary-200 bg-clip-text text-transparent">
                Imaging Platform
              </span>
            </h1>
            <p class="mt-6 max-w-xl text-lg leading-relaxed text-primary-100">
              Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.

            </p>
            <p class="mt-3 text-sm text-primary-300">
              Contact: <a href="mailto:vip-support@creatis.insa-lyon.fr" class="underline hover:text-white">vip-support@creatis.insa-lyon.fr</a>
            </p>
            <div class="mt-10 flex flex-wrap gap-4">
              <RouterLink
                to="/register"
                class="inline-flex items-center gap-2 rounded-xl bg-white px-6 py-3 text-sm font-semibold text-primary-700 shadow-lg transition hover:bg-primary-50 hover:shadow-xl"
              >
                Create an account
                <ArrowRight class="h-4 w-4" />
              </RouterLink>
              <RouterLink
                to="/login"
                class="inline-flex items-center gap-2 rounded-xl border border-white/30 bg-white/10 px-6 py-3 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/20"
              >
                Sign in
              </RouterLink>
            </div>
          </div>

          <!-- Right: Stats grid -->
          <div class="grid grid-cols-2 gap-4">
            <div
              v-for="stat in stats"
              :key="stat.label"
              class="rounded-2xl border border-white/10 bg-white/10 p-6 backdrop-blur-sm"
            >
              <p class="text-4xl font-extrabold text-white">{{ stat.value }}</p>
              <p class="mt-1 text-sm text-primary-200">{{ stat.label }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Wave -->
      <div class="relative -mb-px">
        <svg viewBox="0 0 1440 80" xmlns="http://www.w3.org/2000/svg" class="block w-full" preserveAspectRatio="none">
          <path d="M0,40 C360,80 1080,0 1440,40 L1440,80 L0,80 Z" fill="white" />
        </svg>
      </div>
    </section>

    <!-- About -->
    <section id="about" class="bg-white py-24">
      <div class="mx-auto max-w-7xl px-6">
        <div class="mx-auto max-w-3xl text-center">
          <h2 class="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
            A <span class="text-primary-600">worldwide</span> research platform
          </h2>
          <p class="mt-4 text-lg text-gray-500 leading-relaxed">
            VIP leverages the EGI e-infrastructure to deliver an open service to researchers across
            the globe. Trouver d'autres trucs à dire
          </p>
        </div>

        <div class="mt-16 grid gap-8 sm:grid-cols-3">
          <div class="rounded-2xl border border-gray-100 bg-gray-50 p-8 text-center">
            <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-primary-100">
              <Cpu class="h-7 w-7 text-primary-600" />
            </div>
            <h3 class="text-lg font-semibold text-gray-900">High-throughput computing</h3>
            <p class="mt-2 text-sm text-gray-500 leading-relaxed">
              Distribute your workflows across European computing grids with zero manual configuration.
            </p>
          </div>
          <div class="rounded-2xl border border-gray-100 bg-gray-50 p-8 text-center">
            <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-primary-100">
              <FlaskConical class="h-7 w-7 text-primary-600" />
            </div>
            <h3 class="text-lg font-semibold text-gray-900">Reproducibility</h3>
            <p class="mt-2 text-sm text-gray-500 leading-relaxed">
              Application versioning and full execution traceability for open and reproducible science.
            </p>
          </div>
          <div class="rounded-2xl border border-gray-100 bg-gray-50 p-8 text-center">
            <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-primary-100">
              <BarChart3 class="h-7 w-7 text-primary-600" />
            </div>
            <h3 class="text-lg font-semibold text-gray-900">Large-scale analysis</h3>
            <p class="mt-2 text-sm text-gray-500 leading-relaxed">
              Thousands of executions performed each year across various fields of medical imaging
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Top Applications -->
    <section id="applications" class="bg-gray-50 py-24">
      <div class="mx-auto max-w-7xl px-6">
        <div class="flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <h2 class="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
              Most used applications
            </h2>
            <p class="mt-2 text-gray-500">
              The VIP community's most used tools for medical image processing.
            </p>
          </div>
          <RouterLink
            to="/applications"
            class="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
          >
            Browse all applications
            <ArrowRight class="h-4 w-4" />
          </RouterLink>
        </div>

        <div class="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="app in topApplications"
            :key="app.name"
            class="group relative rounded-2xl border border-gray-200 bg-white p-6 shadow-sm transition-all hover:shadow-md hover:border-primary-200"
          >
            <div class="flex items-start gap-4">
              <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-linear-to-br from-primary-600 to-primary-800 text-white text-lg font-bold shadow">
                {{ app.name.charAt(0) }}
              </div>
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <h3 class="truncate text-base font-bold text-gray-900">{{ app.name }}</h3>
                  <span class="shrink-0 rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-500">
                    {{ app.version }}
                  </span>
                </div>
                <AppBadge :variant="getGroupBadgeColor(app.category)" class="mt-1">
                  {{ app.category }}
                </AppBadge>
              </div>
            </div>

            <p class="mt-4 line-clamp-2 text-sm text-gray-500 leading-relaxed">
              {{ app.description }}
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Publications -->
    <section id="publications" class="bg-white py-24">
      <div class="mx-auto max-w-7xl px-6">
        <div class="flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <h2 class="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
              Latest publications
            </h2>
            <p class="mt-2 text-gray-500">
              Recent works from the community using the VIP platform.
            </p>
          </div>
          <a
            href="https://www.creatis.insa-lyon.fr/vip/news.html"
            target="_blank"
            rel="noopener"
            class="inline-flex items-center gap-2 text-sm font-medium text-primary-600 hover:text-primary-700"
          >
            All publications <ExternalLink class="h-4 w-4" />
          </a>
        </div>

        <div class="mt-10 grid gap-5 sm:grid-cols-2">
          <a
            v-for="pub in publications"
            :key="pub.doi"
            :href="`https://doi.org/${pub.doi}`"
            target="_blank"
            rel="noopener"
            class="group rounded-2xl border border-gray-200 bg-white p-6 shadow-sm transition-all hover:shadow-md hover:border-primary-200"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary-50">
                <BookOpen class="h-5 w-5 text-primary-600" />
              </div>
              <ExternalLink class="h-4 w-4 shrink-0 text-gray-300 transition group-hover:text-primary-400" />
            </div>

            <h3 class="mt-4 text-sm font-semibold leading-snug text-gray-900 group-hover:text-primary-700 transition">
              {{ pub.title }}
            </h3>
            <p class="mt-2 text-xs text-gray-400">{{ pub.authors }}</p>

            <div class="mt-4 flex flex-wrap items-center gap-2">
              <span class="inline-flex items-center gap-1 rounded-full bg-primary-50 px-2.5 py-1 text-xs font-medium text-primary-700">
                <BookOpen class="h-3 w-3" />
                {{ pub.journal }}
              </span>
              <span class="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-1 text-xs text-gray-500">
                <Calendar class="h-3 w-3" />
                {{ pub.year }}
              </span>
              <span
                v-for="tag in pub.tags"
                :key="tag"
                class="rounded-full bg-gray-100 px-2.5 py-1 text-xs text-gray-500"
              >
                {{ tag }}
              </span>
            </div>
          </a>
        </div>
      </div>
    </section>

    <!-- Team -->
    <section id="team" class="bg-gray-50 py-24">
      <div class="mx-auto max-w-7xl px-6">
        <!-- Current team -->
        <div class="text-center">
          <div class="inline-flex items-center gap-2 rounded-full bg-primary-100 px-4 py-1.5 text-sm font-medium text-primary-700">
            <Users class="h-4 w-4" />
            Current team
          </div>
          <h2 class="mt-4 text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
            The people behind VIP
          </h2>
          <p class="mt-3 text-gray-500">
            A research and engineering team based primarily at the CREATIS laboratory, INSA Lyon.
          </p>
        </div>

        <div class="mt-12 grid gap-6 sm:grid-cols-2">
          <div
            v-for="member in permanentTeam"
            :key="member.name"
            class="group rounded-2xl border border-gray-200 bg-white p-6 shadow-sm transition-all hover:shadow-md hover:border-primary-200"
          >
            <div class="flex items-center gap-4">
              <img
                :src="member.photo"
                :alt="member.name"
                class="h-16 w-16 rounded-full object-cover ring-2 ring-gray-100 ring-offset-2 transition group-hover:ring-primary-200"
                loading="lazy"
              />
              <div>
                <h3 class="font-bold text-gray-900">{{ member.name }}</h3>
                <p class="text-sm text-gray-500">{{ member.role }}</p>
                <span class="mt-1 inline-block rounded-full bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-600">
                  {{ member.org }}
                </span>
              </div>
            </div>
            <a
              v-if="member.url"
              :href="member.url"
              target="_blank"
              rel="noopener"
              class="mt-4 inline-flex items-center gap-1 text-xs font-medium text-primary-600 hover:text-primary-700"
            >
              More info <ExternalLink class="h-3 w-3" />
            </a>
          </div>
        </div>

        <div class="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="member in fixedTermTeam"
            :key="member.name"
            class="group rounded-2xl border border-gray-200 bg-white p-6 shadow-sm transition-all hover:shadow-md hover:border-primary-200"
          >
            <div class="flex items-center gap-4">
              <img
                :src="member.photo"
                :alt="member.name"
                class="h-16 w-16 rounded-full object-cover ring-2 ring-gray-100 ring-offset-2 transition group-hover:ring-primary-200"
                loading="lazy"
              />
              <div>
                <h3 class="font-bold text-gray-900">{{ member.name }}</h3>
                <p class="text-sm text-gray-500">{{ member.role }}</p>
                <span class="mt-1 inline-block rounded-full bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-600">
                  {{ member.org }}
                </span>
              </div>
            </div>
            <a
              v-if="member.url"
              :href="member.url"
              target="_blank"
              rel="noopener"
              class="mt-4 inline-flex items-center gap-1 text-xs font-medium text-primary-600 hover:text-primary-700"
            >
              More info <ExternalLink class="h-3 w-3" />
            </a>
          </div>
        </div>

        <!-- Former contributors -->
        <div class="mt-20 text-center">
          <div class="inline-flex items-center gap-2 rounded-full bg-gray-200 px-4 py-1.5 text-sm font-medium text-gray-600">
            Former contributors
          </div>
          <h2 class="mt-4 text-2xl font-bold text-gray-800">
            Those who shaped the project
          </h2>
        </div>

        <div class="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <div
            v-for="member in formerContributors"
            :key="member.name"
            class="rounded-2xl border border-gray-100 bg-white p-5 text-center shadow-sm transition-all hover:shadow-md"
          >
            <img
              :src="member.photo"
              :alt="member.name"
              class="mx-auto h-16 w-16 rounded-full object-cover ring-2 ring-gray-100"
              loading="lazy"
            />
            <h3 class="mt-3 text-sm font-bold text-gray-900">{{ member.name }}</h3>
            <p class="mt-0.5 text-xs text-gray-500">{{ member.role }}</p>
            <span class="mt-1 inline-block rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-500">
              {{ member.org }}
            </span>
            <a
              v-if="member.url"
              :href="member.url"
              target="_blank"
              rel="noopener"
              class="mt-3 flex items-center justify-center gap-1 text-xs font-medium text-primary-600 hover:text-primary-700"
            >
              More info <ExternalLink class="h-3 w-3" />
            </a>
          </div>
        </div>

        <!-- Steering committee -->
        <div class="mt-20">
          <div class="text-center">
            <div class="inline-flex items-center gap-2 rounded-full bg-gray-200 px-4 py-1.5 text-sm font-medium text-gray-600">
              Steering committee
            </div>
            <h2 class="mt-4 text-2xl font-bold text-gray-800">
              Platform governance
            </h2>
          </div>
          <div class="mt-8 flex flex-wrap justify-center gap-4">
            <div
              v-for="member in steeringCommittee"
              :key="member.name"
              class="rounded-2xl border border-gray-100 bg-white px-8 py-5 text-center shadow-sm"
            >
              <div class="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-linear-to-br from-primary-600 to-primary-800 text-lg font-bold text-white shadow">
                {{ member.name.charAt(0) }}
              </div>
              <h3 class="text-sm font-bold text-gray-900">{{ member.name }}</h3>
              <p class="mt-0.5 text-xs text-gray-500">{{ member.role }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="bg-linear-to-br from-primary-800 to-primary-950 text-white">
      <div class="mx-auto max-w-7xl px-6 py-12">
        <div class="flex flex-col items-center gap-6 sm:flex-row sm:justify-between">
          <div class="flex items-center gap-3">
            <img src="/team/vip-logo-without-text.png" alt="VIP Logo" class="h-8 w-auto rounded-sm" />
            <div>
              <p class="font-semibold">VIP Portal</p>
              <p class="text-xs text-primary-300">Virtual Imaging Platform</p>
            </div>
          </div>
          <div class="flex flex-wrap justify-center gap-6 text-sm text-primary-300">
            <a href="https://vip.creatis.insa-lyon.fr/documentation/" target="_blank" rel="noopener" class="transition hover:text-white">Documentation</a>
            <a href="https://vip.creatis.insa-lyon.fr/documentation/privacypolicy.html" target="_blank" rel="noopener" class="transition hover:text-white">Privacy Policy</a>
            <a href="https://vip.creatis.insa-lyon.fr/documentation/terms.html" target="_blank" rel="noopener" class="transition hover:text-white">Terms of Use</a>
            <a href="mailto:vip-support@creatis.insa-lyon.fr" class="transition hover:text-white">Contact</a>
          </div>
        </div>
        <div class="mt-8 border-t border-white/10 pt-6 text-center text-xs text-primary-400">
          © {{ new Date().getFullYear() }} Virtual Imaging Platform - CREATIS, INSA Lyon
        </div>
      </div>
    </footer>

  </div>
</template>
