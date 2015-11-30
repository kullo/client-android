#!/usr/bin/env python3

import io
import json
import urllib.request

impressum_txt = urllib.request.urlopen("https://www.kullo.net/assets/impressum.json").read().decode('utf-8')

impressum = json.loads(impressum_txt)

HEAD = {
  'provider':            { 'de': 'Anbieter',         'en': 'Provider' },
  'representation':      { 'de': 'Vertretung',       'en': 'Representation' },
  'contact':             { 'de': 'Kontakt',          'en': 'Contact' },
  'register':            { 'de': 'Handelsregister',  'en': 'Commercial register' },
  'vatin':               { 'de': 'Umsatzsteuer-Identifikationsnummer',
                           'en': 'VAT identification number' },
  'berlin_office':       { 'de': 'Standort Berlin',  'en': 'Berlin office' },
  'content_responsible': { 'de': 'Verantwortlich für den Inhalt',
                           'en': 'Responsible for the contect' },

}

content = {}

for lang in ['en', 'de']:
    out = io.StringIO()
    print(HEAD['provider'][lang], file=out)
    print(impressum['provider'], file=out)
    print('', file=out)
    print(HEAD['representation'][lang], file=out)
    print(impressum['representation'], file=out)
    print('', file=out)
    print(HEAD['contact'][lang], file=out)
    print(impressum['contact']['phone'], file=out)
    print(impressum['contact']['email'], file=out)
    print(impressum['contact']['kullo'], file=out)
    print('', file=out)
    print(HEAD['register'][lang], file=out)
    print(impressum['register'], file=out)
    print('', file=out)
    print(HEAD['vatin'][lang], file=out)
    print(impressum['vatin'], file=out)
    print('', file=out)
    print(HEAD['berlin_office'][lang], file=out)
    print(impressum['berlin_office'], file=out)

    content[lang] = out.getvalue().strip()
    print("############## Preview " + lang)
    print(content[lang])

for lang in ['en', 'de']:
    print("############## Android " + lang)
    print(content[lang].replace("\n", "\\n"))

