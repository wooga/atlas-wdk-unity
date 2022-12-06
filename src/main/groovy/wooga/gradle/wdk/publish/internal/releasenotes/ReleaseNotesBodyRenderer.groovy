/*
 * Copyright 2021 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.wdk.publish.internal.releasenotes

import com.wooga.github.changelog.render.ChangeRenderer
import com.wooga.github.changelog.render.MarkdownRenderer
import com.wooga.github.changelog.render.markdown.Headline
import com.wooga.github.changelog.render.markdown.HeadlineType
import com.wooga.github.changelog.render.markdown.Link
import org.kohsuke.github.GHPullRequest

class ReleaseNotesBodyRenderer implements ChangeRenderer<ChangeSet>, MarkdownRenderer {

	boolean generateInlineLinks = true
	HeadlineType headlineType = HeadlineType.atx

	@Override
	String render(ChangeSet changeSet) {
		Set<Link> links = new HashSet<Link>()
		StringBuilder content = new StringBuilder();
		if(!changeSet.changes.isEmpty()) {
			def changesContent = renderChangesSubsection(changeSet.changes, links)
			content.append(changesContent)
		} else {
			if (!changeSet.pullRequests.empty) {
				def prsContent = renderPRsSubsection(changeSet.pullRequests, links)
				content.append(prsContent)
				content.append("\n")
			}
		}
		content.append("\n")
		content.append(links.sort({ a, b -> a.text <=> b.text }).collect({ it.reference() }).join("\n"))
		return content.toString();
	}

	private StringBuilder renderChangesSubsection(Map<ChangeNote, GHPullRequest> changes, Set<Link> links) {
		StringBuilder content = new StringBuilder()
		content.append(new Headline("Changes", 2, headlineType))
		changes.keySet().sort {a,b ->
			a.category <=> b.category ?:
		    -(changes[a].number <=> changes[b].number) ?:
			a.text <=> b.text
		}.each { changeNote ->
			def pr = changes[changeNote]
			def prLink = new Link("#" + pr.number, pr.getHtmlUrl())
			def userLink = getUserLink(pr)
			def iconLink = new Link(changeNote.category,
								"https://resources.atlas.wooga.com/icons/icon_${changeNote.category.toLowerCase()}.svg",
									changeNote.category)

			if (!generateInlineLinks) {
				links.add(prLink)
				links.add(userLink)
			}
			links.add(iconLink)

			content.append("* !${iconLink.referenceLink()} ${changeNote.text} ${prLink.link(generateInlineLinks)} ${userLink.link(generateInlineLinks)}\n")
		}
		return content
	}

	private StringBuilder renderPRsSubsection(List<GHPullRequest> pullRequests, Set<Link> links) {
		StringBuilder content = new StringBuilder()
		content.append(new Headline("Pull Requests", 2, headlineType))
		pullRequests.sort {a,b ->
					-(a.number <=> b.number) ?:
					a.text <=> b.text
		}.each { pr ->
			def prLink = new Link("#" + pr.number, pr.getHtmlUrl())
			def userLink = getUserLink(pr)

			if (!generateInlineLinks) {
				links.add(prLink)
				links.add(userLink)
			}
			content.append("* ${prLink.link(generateInlineLinks)}: ${pr.title} ${userLink.link(generateInlineLinks)}\n")
		}
		return content
	}

	static Link getUserLink(GHPullRequest pr) {
		new Link("@" + pr.user.login, "https://github.com/" + pr.user.login)
	}
}
