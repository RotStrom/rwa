package models

import play.api.i18n.Lang

case class Vocabulary(sourceLang: Lang, targetLang: Lang, word: String, translation: String)
