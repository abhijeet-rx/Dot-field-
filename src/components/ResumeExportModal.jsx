import React, { useState } from 'react';

export default function ResumeExportModal({ tailoredData, onClose }) {
  const [copied, setCopied] = useState(false);

  if (!tailoredData) return null;

  const { summary, skills, experience, education, projects, tailoringAnalysis } = tailoredData;

  const resumeMarkdown = `
# Professional Summary
${summary || ''}

## Skills & Keywords
${skills?.technicalSkills?.join(', ') || ''}

## Experience
${(experience || []).map(exp => `### ${exp.role} - ${exp.company}\n${exp.tailoredBullets?.map(b => `- ${b}`).join('\n') || ''}`).join('\n\n')}

## Education
${(education || []).map(edu => `- ${edu.degree}, ${edu.institution}`).join('\n')}

## Projects
${(projects || []).map(p => `### ${p.name}\n${p.description || ''}`).join('\n\n')}
  `.trim();

  function handleCopy() {
    navigator.clipboard.writeText(resumeMarkdown);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  function handleDownload() {
    const blob = new Blob([resumeMarkdown], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Tailored_Resume_${tailoredData.jobId || 'Job'}.md`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1100 }}>
      <div style={{ background: '#1e1b4b', padding: '24px', borderRadius: '16px', width: '700px', maxHeight: '85vh', display: 'flex', flexDirection: 'column', border: '1px solid rgba(255,255,255,0.1)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 600 }}>📄 Tailored Resume Preview</h3>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', fontSize: '1.2rem', cursor: 'pointer' }}>×</button>
        </div>

        {tailoringAnalysis && (
          <div style={{ background: 'rgba(168, 85, 247, 0.15)', border: '1px solid rgba(168, 85, 247, 0.3)', padding: '12px', borderRadius: '8px', marginBottom: '16px', fontSize: '0.85rem' }}>
            ✨ <strong>Tailoring Rationales:</strong> {tailoringAnalysis.relevanceSummary || 'Optimized from authentic candidate profile'}
          </div>
        )}

        <div style={{ flex: 1, overflowY: 'auto', background: 'rgba(0,0,0,0.3)', padding: '16px', borderRadius: '8px', fontFamily: 'monospace', fontSize: '0.85rem', whiteSpace: 'pre-wrap', color: '#e2e8f0', marginBottom: '16px' }}>
          {resumeMarkdown}
        </div>

        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
          <button className="btn-secondary" onClick={handleCopy}>
            {copied ? '✓ Copied!' : 'Copy Markdown'}
          </button>
          <button className="btn-primary" onClick={handleDownload}>
            Download .md
          </button>
        </div>
      </div>
    </div>
  );
}
