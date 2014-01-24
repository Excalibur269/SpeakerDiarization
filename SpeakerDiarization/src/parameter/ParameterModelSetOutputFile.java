/**
 * 
 * <p>
 * ParameterModelSetOutputFile
 * </p>
 * 
 * @author <a href="mailto:sylvain.meignier@lium.univ-lemans.fr">Sylvain Meignier</a>
 * @version v2.0
 * 
 *          Copyright (c) 2007-2009 Universite du Maine. All Rights Reserved. Use is subject to license terms.
 * 
 *          THIS SOFTWARE IS PROVIDED BY THE "UNIVERSITE DU MAINE" AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *          DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *          USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *          ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *          not more use
 */

package parameter;

/**
 * The Class ParameterModelSetOutputFile.
 */
public class ParameterModelSetOutputFile extends ParameterModelSet implements Cloneable {

	/** The Constant DefaultMask. */
	private static final String DefaultMask = "%s.out.gmms";

	/**
	 * Instantiates a new parameter model set output file.
	 * 
	 * @param parameter the parameter
	 */
	public ParameterModelSetOutputFile(Parameter parameter) {
		super(parameter);
		setMask(getDefaultMask());
		type = "Output";
		addOption(new LongOptWithAction("t" + type + "Mask", new ActionMask(), ""));
		addOption(new LongOptWithAction("t" + type + "ModelType", new ActionFormat(), ""));
	}

	/*
	 * (non-Javadoc)
	 * @see fr.lium.spkDiarization.parameter.ParameterModelSet#clone()
	 */
	@Override
	protected ParameterModelSetOutputFile clone() throws CloneNotSupportedException {
		return (ParameterModelSetOutputFile) super.clone();
	}

	/**
	 * Gets the default mask.
	 * 
	 * @return the default mask
	 */
	public static String getDefaultMask() {
		return DefaultMask;
	}
}