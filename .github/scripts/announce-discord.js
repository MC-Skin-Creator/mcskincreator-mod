#!/usr/bin/env node
/*
 * Announces a released version of the mod on Discord, from the changelog
 * (changelog/{en,fr,es}.md). No dependency and no AI: the text is what the pull
 * requests wrote.
 *
 *   node .github/scripts/announce-discord.js --version 0.5.0   post it
 *   node .github/scripts/announce-discord.js --version 0.5.0 --dry   print, post nothing
 *
 * Webhooks are repository secrets, all optional: DISCORD_WEBHOOK_EN, _FR, _ES (one
 * channel per language), or DISCORD_WEBHOOK alone, which serves English. With none,
 * the script says so and exits cleanly.
 *
 * The version must have its own dated section in the file: a release that carries
 * nothing for the players announces nothing, and an older version is never
 * announced again by mistake.
 */
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const LANGS = ['en', 'fr', 'es'];
const PROJECT = 'Mod';
const SITE_URL = process.env.MCSC_SITE_URL || 'https://mcskincreator.app/changelog';

const KINDS = {
  new: 'new', nouveautés: 'new', novedades: 'new',
  improved: 'improved', améliorations: 'improved', mejoras: 'improved',
  fixed: 'fixed', corrections: 'fixed', correcciones: 'fixed'
};
const HEADINGS = {
  en: { new: 'New', improved: 'Improved', fixed: 'Fixed' },
  fr: { new: 'Nouveautés', improved: 'Améliorations', fixed: 'Corrections' },
  es: { new: 'Novedades', improved: 'Mejoras', fixed: 'Correcciones' }
};
const LINK = { en: 'Full changelog', fr: 'Journal complet', es: 'Registro completo' };
const IMAGE = /^\s+!\[[^\]]*\]\((images\/[a-z0-9][a-z0-9._-]*\.(?:png|webp|jpe?g|gif))\)\s*$/;
// Discord loads the picture from a public address: the file on main, where the release commit just landed
const IMAGE_BASE = process.env.MOD_IMAGE_BASE || 'https://raw.githubusercontent.com/MC-Skin-Creator/mcskincreator-mod/main/changelog/images/';
const UNRELEASED = /^(unreleased|prochaine version|próxima versión)$/i;
const RELEASE = /^(\d+\.\d+\.\d+[^\s]*)\s*[—–-]\s*(\d{4}-\d{2}-\d{2})$/;

function parse(markdown) {
  const releases = [];
  let release = null, section = null, open = null;
  const close = () => { if (open && section) section.items.push(open.join(' ').trim()); open = null; };
  for (const line of markdown.split(/\r?\n/)) {
    const h2 = /^##\s+(.+?)\s*$/.exec(line);
    const h3 = /^###\s+(.+?)\s*$/.exec(line);
    if (h2) {
      close(); section = null;
      if (UNRELEASED.test(h2[1])) release = { version: null, date: null, sections: [] };
      else {
        const r = RELEASE.exec(h2[1]);
        release = r ? { version: r[1], date: r[2], sections: [] } : null;
      }
      if (release) releases.push(release);
    } else if (h3) {
      close();
      const kind = KINDS[h3[1].toLowerCase()];
      section = release && kind ? { kind, items: [] } : null;
      if (section) release.sections.push(section);
    } else if (/^-\s+/.test(line) && section) {
      close(); open = [line.replace(/^-\s+/, '')];
    } else if (open && IMAGE.test(line)) {
      // a screenshot is not text: the first one of the version illustrates the embed
      if (release && !release.image) release.image = IMAGE.exec(line)[1];
    } else if (open && /^\s+\S/.test(line)) {
      open.push(line.trim());
    } else if (!line.trim()) {
      close();
    }
  }
  close();
  return releases;
}

const markdownOf = (release, lang) => release.sections
  .map(s => '**' + HEADINGS[lang][s.kind] + '**\n' + s.items.map(i => '• ' + i).join('\n'))
  .join('\n\n');

function payload(release, lang) {
  const link = '\n\n[' + LINK[lang] + '](' + SITE_URL + ')';
  let text = markdownOf(release, lang);
  const room = 4096 - link.length - 2;
  if (text.length > room) {
    text = text.slice(0, room);
    text = text.slice(0, text.lastIndexOf('\n')) + '\n…';
  }
  const embed = {
    title: 'MC Skin Creator ' + PROJECT + ' ' + release.version,
    url: SITE_URL,
    description: text + link,
    color: 0x3a8f2a,
    timestamp: release.date + 'T12:00:00.000Z',
    footer: { text: 'mcskincreator.app' }
  };
  if (release.image) embed.image = { url: IMAGE_BASE + release.image.replace(/^images\//, '') };
  return { username: 'MC Skin Creator', allowed_mentions: { parse: [] }, embeds: [embed] };
}

const webhook = lang => process.env['DISCORD_WEBHOOK_' + lang.toUpperCase()]
  || (lang === 'en' ? process.env.DISCORD_WEBHOOK : '') || '';

async function main(argv) {
  const dry = argv.includes('--dry');
  const v = argv.indexOf('--version');
  const version = v >= 0 ? argv[v + 1] : '';
  if (!version) {
    console.error('usage: announce-discord.js --version X.Y.Z [--dry]');
    return 2;
  }
  let failed = 0;
  for (const lang of LANGS) {
    const file = path.join(ROOT, 'changelog', lang + '.md');
    const release = fs.existsSync(file) ? parse(fs.readFileSync(file, 'utf8')).find(r => r.version === version) : null;
    if (!release) {
      console.log('::notice::changelog/' + lang + '.md has no section for ' + version + ': nothing to announce in ' + lang + '.');
      continue;
    }
    const body = payload(release, lang);
    if (dry) { console.log('--- ' + lang + ' ---\n' + JSON.stringify(body, null, 2)); continue; }
    const url = webhook(lang);
    if (!url) { console.log(lang + ': no webhook, skipped.'); continue; }
    const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    if (res.ok) console.log(lang + ': ' + PROJECT + ' ' + version + ' announced.');
    else { failed = 1; console.log('::error::' + lang + ': Discord answered ' + res.status + ' ' + (await res.text()).slice(0, 200)); }
  }
  return failed;
}

main(process.argv.slice(2)).then(code => process.exit(code), e => { console.error(e); process.exit(1); });
